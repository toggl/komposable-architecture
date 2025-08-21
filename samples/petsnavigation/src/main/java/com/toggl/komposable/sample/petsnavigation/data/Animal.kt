package com.toggl.komposable.sample.petsnavigation.data

data class Animal(val id: Int, val name: String, val description: String)

val sampleAnimals = listOf(
    Animal(4, "Waddles", "A bird that forgot how to bird, so it decided to cosplay as a well-dressed, slightly tipsy waiter."),
    Animal(5, "Bandit", "Nature's most wanted burglar. Will trade your garbage for adorable, unblinking stares. Washes its food, and your heart, before stealing both."),
    Animal(6, "Chillbert", "The undisputed zen master of the animal kingdom. Its only two modes are 'napping' and 'letting other animals nap on it'. Basically a sentient, furry beanbag chair."),
    Animal(7, "Speedy", "Moves at the speed of continental drift. Its life goal is to perfect the art of doing absolutely nothing, and it's winning."),
    Animal(8, "Quilliam", "A walking cactus with a permanently worried expression. Don't try to hug it, no matter how much it looks like it needs one."),
    Animal(9, "Confusious", "Proof that nature sometimes just clicks 'randomize' on the character creation screen. A duck-billed, beaver-tailed, egg-laying mammal with venomous spurs."),
    Animal(10, "Neville", "The living embodiment of a bad attitude. Runs on pure spite and scorpion venom. Has zero chill and a very, very long list of enemies.")
)
