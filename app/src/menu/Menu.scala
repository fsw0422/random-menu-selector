package src.menu
import java.util.UUID

case class Menu(UUID: UUID,
                typ: String,
                name: String,
                ingredients: Seq[String],
                recipe: List[String],
                link: String)
