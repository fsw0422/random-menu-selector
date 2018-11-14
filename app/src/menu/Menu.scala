package src.menu

case class Menu(typ: String,
                name: String,
                ingredients: Seq[String],
                recipe: List[String],
                link: String)
