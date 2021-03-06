package models

import scalikejdbc._
import skinny.orm.{Alias, SkinnyCRUDMapperWithId}
import utils.MissionTime

case class Score(
    id: Long,
    missionId: Long,
    accountId: Long,
    time: MissionTime,
    distance: Double,
    money: Int,
    rate: Int,
    created: Long,
    account: Option[Account] = None
) {
  def save()(implicit session: DBSession): Long = Score.save(this)
}

object Score extends SkinnyCRUDMapperWithId[Long, Score] {
  override def idToRawValue(id: Long): Any = id
  override def rawValueToId(value: Any): Long = value.toString.toLong

  override def defaultAlias: Alias[Score] = createAlias("sc")

  override def extract(rs: WrappedResultSet, n: ResultName[Score]): Score = autoConstruct(rs, n, "account")

  lazy val accountRef = belongsTo[Account](
    right = Account,
    merge = (s, a) => s.copy(account = a)
  )

  def save(score: Score)(implicit session: DBSession): Long = {
    MissionRate.upsert(score.missionId, score.rate)
    createWithAttributes(
      'missionId -> score.missionId,
      'accountID -> score.accountId,
      'time -> score.time.toString,
      'money -> score.money,
      'distance -> score.distance,
      'rate -> score.rate,
      'created -> score.created
    )
  }
}
