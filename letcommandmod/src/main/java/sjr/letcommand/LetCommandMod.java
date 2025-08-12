package sjr.letcommand;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.util.math.Vec2f; // 添加Vec2f导入

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.*;

public class LetCommandMod implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerLetCommand(dispatcher);
        });
    }

    private void registerLetCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("let")
            .requires(source -> source.hasPermissionLevel(2)) // 需要OP权限
            .then(argument("target", EntityArgumentType.entities())
                .then(literal("say")
                    .then(argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            return handleSayCommand(
                                ctx,
                                EntityArgumentType.getEntities(ctx, "target"),
                                StringArgumentType.getString(ctx, "text")
                            );
                        })
                    )
                )
                .then(literal("do")
                    .then(argument("lv", IntegerArgumentType.integer(0, 4))
                        .then(argument("command", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                return handleDoCommand(
                                    ctx,
                                    EntityArgumentType.getEntities(ctx, "target"),
                                    IntegerArgumentType.getInteger(ctx, "lv"),
                                    StringArgumentType.getString(ctx, "command")
                                );
                            })
                        )
                    )
                )
            )
        );
    }

    private int handleSayCommand(CommandContext<ServerCommandSource> ctx, 
                                Collection<? extends Entity> targets, 
                                String text) {
        int count = 0;
        for (Entity entity : targets) {
            Text message = Text.literal("<" + entity.getName().getString() + "> " + text);
            
            // 使用广播API
            ctx.getSource().getServer().getPlayerManager().broadcast(
                message, 
                false
            );
            count++;
        }
        return count;
    }

    private int handleDoCommand(CommandContext<ServerCommandSource> ctx, 
                               Collection<? extends Entity> targets, 
                               int permissionLevel, 
                               String command) {
        int count = 0;
        for (Entity entity : targets) {
            // 修复旋转参数 - 使用Vec2f对象
            Vec2f rotation = new Vec2f(entity.getPitch(), entity.getYaw());
            
            ServerCommandSource commandSource = ctx.getSource()
                .withEntity(entity)
                .withLevel(permissionLevel)
                .withPosition(entity.getPos())
                .withRotation(rotation); // 使用Vec2f对象
            
            try {
                ctx.getSource().getServer().getCommandManager().executeWithPrefix(
                    commandSource,
                    command
                );
                count++;
            } catch (Exception e) {
                ctx.getSource().sendError(Text.literal("执行失败: " + e.getMessage()));
            }
        }
        return count;
    }
}