package io.dedyn.engineermantra.omega.shared

import java.awt.Color

object MessageLevel{
    private interface ILevel{
        val color: Color
    }
    enum class Level: ILevel {
        CREATE{
            override val color: Color = Color.GREEN
        },
        MODIFY{
            override val color: Color = Color.BLUE
        },
        DELETE {
            override val color: Color = Color.RED
        },
        KICK {
            override val color: Color = Color.YELLOW
        },
        BAN{
            override val color: Color = Color.BLACK
        }
    }
}
