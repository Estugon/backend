package sc.protocol

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute

/** Indicates that the receiving Player left or was kicked from the game. */
@XStreamAlias(value = "left")
data class RemovedFromGame(
        @XStreamAsAttribute
        val roomId: String
): ResponsePacket
