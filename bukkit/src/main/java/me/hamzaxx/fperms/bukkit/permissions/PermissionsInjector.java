package me.hamzaxx.fperms.bukkit.permissions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class PermissionsInjector
{
    private static Field permField;
    private Player player;
    private fPermsPermissible permissible;

    public PermissionsInjector(Player player, fPermsPermissible permissible)
    {
        this.player = player;
        this.permissible = permissible;
    }

    static
    {
        String version = Bukkit.getServer().getClass().getPackage().getName().split( "\\." )[ 3 ];
        try
        {
            Class<?> craftHumanEntityClass = Class.forName( "org.bukkit.craftbukkit." + version + ".entity.CraftHumanEntity" );
            permField = craftHumanEntityClass.getDeclaredField( "perm" );
            permField.setAccessible( true );
        } catch ( ClassNotFoundException | NoSuchFieldException e )
        {
            e.printStackTrace();
        }
    }

    public void inject()
    {
        try
        {
            permField.set( player, permissible );
        } catch ( IllegalAccessException e )
        {
            e.printStackTrace();
        }
    }
}
