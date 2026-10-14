package com.pixelmonmod.pixelmon.client.gui.npc;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import java.util.Collections;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

/** Private QA screen: exercises the shipped native dialogue helper without spawning an NPC. */
public final class BackportDialogueLayoutProbe extends Screen {
    public static final String TEXT="听说想获得高个体值宝可梦，培育是个好办法。亲代可以把自己的个体值遗传给宝可梦蛋！";
    public BackportDialogueLayoutProbe(){super(new StringTextComponent("Dialogue QA"));}
    @Override public void func_230430_a_(MatrixStack matrix,int mouseX,int mouseY,float partialTick){
        func_230446_a_(matrix);
        ScreenHelper.drawDialogueBox(matrix,this,"索菲娅",Collections.singletonList(TEXT),1f);
    }
}
