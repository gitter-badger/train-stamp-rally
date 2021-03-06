package controllers

import play.api.mvc.Results

object Responses extends Results {
  val JSONParseError = BadRequest("JSON parse error.")
  val Success = Ok("Success")
  def notFound(name: String) = Results.NotFound(s"#{name} not found.")
}
