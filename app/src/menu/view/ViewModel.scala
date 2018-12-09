package src.menu.view

import play.api.libs.json.Json

object MenuView {
  implicit val jsonFormat = Json.format[MenuView]
}

case class MenuView(name: String,
                    ingredients: List[String],
                    recipe: String,
                    link: String)
