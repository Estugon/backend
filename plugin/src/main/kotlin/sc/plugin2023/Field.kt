package sc.plugin2023

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamConverter
import sc.api.plugins.IField
import sc.api.plugins.Team
import sc.plugin2023.util.FieldConverter

@XStreamConverter(FieldConverter::class)
@XStreamAlias("field")
data class Field(val fish: Int = 0, val penguin: Team? = null) : IField<Field> {
    override val isEmpty: Boolean
        get() = fish == 0 && penguin == null
    override val isOccupied: Boolean
        get() = penguin != null
    
    override fun clone(): Field = Field(fish, penguin)
    
    override fun toString(): String = penguin?.letter?.toString() ?: fish.toString()
}
