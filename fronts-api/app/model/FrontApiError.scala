package model

trait FrontApiError {
  def getMessage: String
}

case class PositionNotFound() extends FrontApiError {
  def getMessage = "Position Not Found"
}

case class DatabaseError() extends FrontApiError {
  def getMessage = "Database Error"
}

case class Success()