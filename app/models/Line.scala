package models

import scalikejdbc._
import skinny.orm.{Alias, SkinnyCRUDMapperWithId}

case class Line(id: Long, name: String) {
  def save()(implicit session: DBSession): Long = Line.save(this)
}

object Line extends SkinnyCRUDMapperWithId[Long, Line] {
  override def defaultAlias: Alias[Line] = createAlias("l")

  override def extract(rs: WrappedResultSet, n: ResultName[Line]): Line = autoConstruct(rs, n)

  override def idToRawValue(id: Long): Any = id
  override def rawValueToId(value: Any): Long = value.toString.toLong

  def save(line: Line)(implicit session: DBSession): Long =
    createWithAttributes('name -> line.name)
}
