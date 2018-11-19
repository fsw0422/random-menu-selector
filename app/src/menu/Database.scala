package src.menu
import java.util.UUID

import scala.concurrent.Future

object Database {

  val menus = Future {
    List(
      Menu(
        UUID.fromString("f3fac000-ea5d-11e8-9f32-f2801f1b9fd1"),
        "Asian",
        "Udon Noodle Bowl",
        Seq(
          "1 package (9 – 12 oz.) udon noodles",
          "1 tablespoon sesame oil",
          "1 small onion, sliced",
          "8 oz. mushrooms, sliced",
          "1 large or 3 small zucchini, cut in half and sliced",
          "3 – 4 scallions, cut into 1 inch pieces",
          "large handful bean sprouts",
          "handful of basil, whole leaf or roughly chopped",
          "1/4 cup water or vegetable broth",
          "3 tablespoons tamari, coconut amino’s or soy sauce",
          "2 teaspoons pure maple syrup",
          "1/2 teaspoon garlic powder",
          "1/2 teaspoon red pepper flakes, or to taste",
          "toasted sesame seeds, to garnish",
        ),
        List(
          "Cook noodles as directed on package, set aside.\n\nWhile you’re waiting for the water to boil, begin slicing up your veggies.\n\nIn a large skillet, heat oil over medium heat, add onion (not the scallions) and cook about 4 minutes or until onions soften. Add mushrooms and cook another 3 minutes. Add the zucchini, scallions, bean sprouts, basil, water/broth, tamari, maple syrup, garlic powder and red pepper flakes, cook another 3 – 4 minutes, or until zucchini has just softened. A cover will help cook your vegetables faster. Remove from heat, add noodles to the pan and toss together. You can also keep the noodles and vegetable mixture separate if you like and mix in your bowl.\n\nServe in individual bowls with a little of the juice from the bottom of the wok/pan. Top with toasted sesame seeds (and more red pepper flakes if you like). A little sriracha wouldn’t hurt either!\nput noodle in hot water"
        ),
        "https://simple-veganista.com/2015/06/simple-udon-noodle-bow.html#tasty-recipes-8539"
      ),
    )
  }(MenuRepository.actorSystem.dispatcher)
}
