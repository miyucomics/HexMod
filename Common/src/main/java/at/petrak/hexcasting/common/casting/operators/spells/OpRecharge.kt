package at.petrak.hexcasting.common.casting.operators.spells

import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.spell.ParticleSpray
import at.petrak.hexcasting.api.spell.RenderedSpell
import at.petrak.hexcasting.api.spell.SpellAction
import at.petrak.hexcasting.api.spell.casting.CastingContext
import at.petrak.hexcasting.api.spell.getItemEntity
import at.petrak.hexcasting.api.spell.iota.Iota
import at.petrak.hexcasting.api.spell.mishaps.MishapBadItem
import at.petrak.hexcasting.api.spell.mishaps.MishapBadOffhandItem
import at.petrak.hexcasting.api.utils.extractMedia
import at.petrak.hexcasting.api.utils.isMediaItem
import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack

object OpRecharge : SpellAction {
    override val argc = 1
    override fun execute(
        args: List<Iota>,
        ctx: CastingContext
    ): Triple<RenderedSpell, Int, List<ParticleSpray>>? {
        val entity = args.getItemEntity(0, argc)

        val (handStack, hand) = ctx.getHeldItemToOperateOn {
            val mana = IXplatAbstractions.INSTANCE.findManaHolder(it)
            mana != null && mana.canRecharge() && mana.insertMedia(-1, true) != 0
        }

        val mana = IXplatAbstractions.INSTANCE.findManaHolder(handStack)

        if (mana == null || !mana.canRecharge())
            throw MishapBadOffhandItem.of(
                handStack,
                hand,
                "rechargable"
            )

        ctx.assertEntityInRange(entity)

        if (!isMediaItem(entity.item)) {
            throw MishapBadItem.of(
                entity,
                "mana"
            )
        }

        if (mana.insertMedia(-1, true) == 0)
            return null

        return Triple(
            Spell(entity, handStack),
            MediaConstants.SHARD_UNIT,
            listOf(ParticleSpray.burst(entity.position(), 0.5))
        )
    }

    private data class Spell(val itemEntity: ItemEntity, val stack: ItemStack) : RenderedSpell {
        override fun cast(ctx: CastingContext) {
            val mana = IXplatAbstractions.INSTANCE.findManaHolder(stack)

            if (mana != null && itemEntity.isAlive) {
                val entityStack = itemEntity.item.copy()

                val emptySpace = mana.insertMedia(-1, true)

                val manaAmt = extractMedia(entityStack, emptySpace)

                mana.insertMedia(manaAmt, false)

                itemEntity.item = entityStack
                if (entityStack.isEmpty)
                    itemEntity.kill()
            }
        }
    }
}
