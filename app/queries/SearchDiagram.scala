package queries

import models._
import play.api.data.Form
import play.api.data.Forms._
import responses.{DiagramResponse, Page, TrainResponse, WithPage}
import scalikejdbc._
import skinny.Pagination
import utils.TrainTime

import scala.collection.breakOut

sealed abstract class SearchDiagram {
  def search()(implicit session: DBSession): Any
  def tuple: (Option[Long], Option[String], Option[Int], Option[Int], Option[String])
}

object SearchDiagram {
  case object All extends SearchDiagram {
    override def search()(implicit session: DBSession): Any = {
      import Diagram.{trainRef, stopStationRef, defaultAlias => d}
      Diagram.joins(trainRef, stopStationRef).findAll(Seq(d.id.desc))
          .map(DiagramResponse.fromDiagram)
    }

    override def tuple = (None, None, None, None, None)
  }

  case class Paging(pageNo: Int, size: Int, lineName: Option[String]) extends SearchDiagram {
    override def search()(implicit session: DBSession): WithPage[Seq[DiagramResponse]] = {
      import Diagram.{trainRef, stopStationRef, defaultAlias => d}
      val pagination = Pagination.page(pageNo).per(size)
      val where = lineName.map(whereLineName).getOrElse(sqls"true")
      val data = Diagram.joins(trainRef, stopStationRef)
          .findAllByWithPagination(where, pagination, Seq(d.id.desc))
          .map(DiagramResponse.fromDiagram)
      val count = Diagram.countBy(where)
      val page = Page(
        total = count,
        size = size,
        current = pageNo,
        last = ((count + size - 1) / size).toInt
      )
      WithPage(page, data)
    }

    private def whereLineName(name: String)(implicit session: DBSession): SQLSyntax = {
      val l = Line.defaultAlias
      val lineIds = Line.findAllBy(sqls.like(Line.column.name, s"%${name}%")).map(_.id)
      val lineStationIds = LineStation.findAllBy(sqls.in(LineStation.column.lineId, lineIds)).map(_.id)
      val diagramIds = StopStation.findAllBy(sqls.in(StopStation.column.lineStationId, lineStationIds)).map(_.diagramId)
      sqls.in(Diagram.defaultAlias.id, diagramIds)
    }

    override def tuple = (None, None, Some(pageNo), Some(size), lineName)
  }

  case class StationSearch(stationId: Long) extends SearchDiagram {
    override def search()(implicit session: DBSession) = {
      val diagramIds = findDiagramIds(stationId)
      Diagram.joins(Diagram.stopStationRef).findAllByIds(diagramIds:_*)
    }

    override def tuple = (Some(stationId), None, None, None, None)
  }

  case class TimeSearch(stationId: Long, time: TrainTime) extends SearchDiagram {
    override def search()(implicit session: DBSession) = {
      val lineStations = LineStation.findAllBy(sqls.eq(LineStation.column.stationId, stationId))
      val stops = StopStation.findAllBy(sqls.in(StopStation.column.lineStationId, lineStations.map(_.id)))
      val diagramIds = stops.map(_.diagramId).distinct
      val lineStationIdTable: Map[Long, Long] = stops.map { ls => ls.diagramId -> ls.lineStationId }(breakOut)
      val diagrams = Diagram.joins(Diagram.stopStationRef, Diagram.trainRef).findAllByIds(diagramIds: _*)
      diagrams.flatMap { d =>
        d.nextTrain(lineStationIdTable(d.id), time).map { train =>
          TrainResponse.fromTrainDiagram(train, d)
        }
      }
    }

    override def tuple  = (Some(stationId), Some(time.toString), None, None, None)
  }

  def apply(
      stationIdOpt: Option[Long],
      timeOpt: Option[String],
      pageNoOpt: Option[Int],
      sizeOpt: Option[Int],
      lineName: Option[String]): SearchDiagram = {
    stationIdOpt.fold {
      pageNoOpt.fold(All: SearchDiagram) { pageNo =>
        Paging(pageNo, sizeOpt.getOrElse(10), lineName): SearchDiagram
      }
    } { stationId =>
      timeOpt.flatMap(TrainTime.fromString)
          .fold(StationSearch(stationId): SearchDiagram) { time => TimeSearch(stationId, time): SearchDiagram }
    }
  }

  def unapply(sd: SearchDiagram): Option[(Option[Long], Option[String], Option[Int], Option[Int], Option[String])] =
    Some(sd.tuple)

  private[this] def findDiagramIds(stationId: Long)(implicit session: DBSession): Seq[Long] = {
    val lineStations = LineStation.findAllBy(sqls.eq(LineStation.column.stationId, stationId))
    val stops = StopStation.findAllBy(sqls.in(StopStation.column.lineStationId, lineStations.map(_.id)))
    stops.map(_.diagramId).distinct
  }

  val form = Form(
    mapping(
      "station" -> optional(longNumber(min = 0L)),
      "time" -> optional(text(minLength = 4, maxLength = 4)),
      "page" -> optional(number(min = 0)),
      "size" -> optional(number(min = 1)),
      "lineName" -> optional(text(minLength = 1))
    )(SearchDiagram.apply)(SearchDiagram.unapply)
  )
}
