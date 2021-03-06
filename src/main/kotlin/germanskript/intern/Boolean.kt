package germanskript.intern

import germanskript.BuildIn
import kotlin.Boolean

class Boolean(val boolean: Boolean): Objekt(BuildIn.IMMKlassen.boolean, BuildIn.Klassen.boolean) {
  override fun toString(): String = if (this.boolean) "wahr" else "falsch"

  override fun equals(other: Any?): Boolean {
    if (other !is germanskript.intern.Boolean) return false
    return boolean == other.boolean
  }

  override fun hashCode() = boolean.hashCode()
}