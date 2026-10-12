package com.walkspot.app.data.find

import com.walkspot.app.data.local.FindAlternative
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.FindKind

/** What a find may be: the name it has now first, then the other guesses of the service. */
fun FindEntity.options(): List<FindAlternative> =
    listOfNotNull(name?.let { FindAlternative(it, latinName, match ?: 0f) }) + alternatives

/**
 * The find named by another of its [options]; the name it had goes back among the others. Its
 * description belonged to the old name and is looked up again.
 */
fun FindEntity.choosing(index: Int): FindEntity {
    val all = options()
    val chosen = all.getOrNull(index)?.takeIf { index > 0 } ?: return this
    return copy(
        name = chosen.name,
        latinName = chosen.latinName,
        // A place has no match, and a name the walker typed has none either.
        match = chosen.match.takeIf { kind != FindKind.PLACE && it > 0 },
        alternatives = all - chosen,
        description = null,
        wikipediaUrl = null,
    )
}

/** The find said to be of another [kind]: what the service of the old kind found holds no longer. */
fun FindEntity.asKind(kind: FindKind): FindEntity {
    if (kind == this.kind) return this
    return copy(
        kind = kind,
        name = null,
        latinName = null,
        match = null,
        alternatives = emptyList(),
        description = null,
        wikipediaUrl = null,
    )
}

/** The find under a name of the walker's own; the guesses of the service stay for a change of mind. */
fun FindEntity.renamed(text: String): FindEntity {
    val typed = text.trim()
    if (typed.isEmpty() || typed == name) return this
    return copy(
        name = typed,
        latinName = null,
        match = null,
        alternatives = options(),
        description = null,
        wikipediaUrl = null,
    )
}
