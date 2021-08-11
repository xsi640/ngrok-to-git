package com.github.xsi640

object ListExtensions

fun List<String>.equals(list: List<String>): Boolean {
    if (this.size != list.size)
        return true

    val pairList = this.zip(list)

    return pairList.all { (elt1, elt2) ->
        elt1 == elt2
    }
}