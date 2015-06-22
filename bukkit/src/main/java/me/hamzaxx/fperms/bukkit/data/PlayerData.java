/*
 * Copyright (c) Effective Light 2015.
 * All rights reserved.
 */

package me.hamzaxx.fperms.bukkit.data;

import me.hamzaxx.fperms.bukkit.fPermsPlugin;
import me.hamzaxx.fperms.shared.permissions.Permission;
import me.hamzaxx.fperms.shared.permissions.PermissionData;
import org.bukkit.Bukkit;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class PlayerData implements Data, Serializable
{

    private fPermsPlugin plugin;
    private String groupName;
    private String prefix;
    private String suffix;

    private Map<String, Permission> permissions;
    private Map<String, Permission> effectivePermissions;

    public PlayerData(fPermsPlugin plugin, String groupName, String prefix,
                      String suffix, Map<String, Permission> permissions)
    {
        this.plugin = plugin;
        this.groupName = groupName;
        this.prefix = prefix;
        this.suffix = suffix;
        this.permissions = permissions;
        effectivePermissions = new HashMap<>();
        recalculatePermissions();
    }

    public PlayerData(fPermsPlugin plugin, PermissionData data)
    {
        this.plugin = plugin;
        this.groupName = data.getName();
        this.prefix = data.getPrefix();
        this.suffix = data.getSuffix();
        this.permissions = data.getPermissions();
        effectivePermissions = new HashMap<>();
        recalculatePermissions();
    }

    @Override
    public String getName()
    {
        return groupName;
    }

    public void setGroup(String groupName)
    {
        this.groupName = groupName;
        recalculatePermissions();
    }

    public GroupData getGroup()
    {
        return plugin.getGroups().get( getName() );
    }

    @Override
    public String getPrefix()
    {
        return prefix;
    }

    @Override
    public String getSuffix()
    {
        return suffix;
    }

    @Override
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    @Override
    public void setSuffix(String suffix)
    {
        this.suffix = suffix;
    }

    @Override
    public void setPermission(Permission permission)
    {
        permissions.put( permission.getName(), permission );
        effectivePermissions.values().forEach( perms -> {
            org.bukkit.permissions.Permission perm = Bukkit.getPluginManager().getPermission( perms.getName() );
            perm.getChildren().entrySet().stream().forEach( entry -> {
                if ( !effectivePermissions.containsKey( perm.getName() ) )
                    effectivePermissions.put( perm.getName(), new Permission( perm.getName(), perms.getLocation(), entry.getValue() ) );
            } );
        } );
    }

    @Override
    public void unsetPermission(String permission)
    {
        permissions.remove( permission );
        recalculatePermissions();
    }

    @Override
    public void setPermissions(Map<String, Permission> permissions)
    {
        this.permissions = permissions;
    }

    @Override
    public Map<String, Permission> getPermissions()
    {
        return permissions;
    }

    public Map<String, Permission> getEffectivePermissions()
    {
        return effectivePermissions;
    }

    public void recalculatePermissions()
    {
        effectivePermissions.clear();
        effectivePermissions.putAll( permissions );
        effectivePermissions.putAll( getGroup().getPermissions() );
        effectivePermissions.values().forEach( permission -> {
            org.bukkit.permissions.Permission perm = Bukkit.getPluginManager().getPermission( permission.getName() );
            perm.getChildren().entrySet().stream().forEach( entry -> {
                if ( !effectivePermissions.containsKey( perm.getName() ) )
                    effectivePermissions.put( perm.getName(), new Permission( perm.getName(), permission.getLocation(), entry.getValue() ) );
            } );
        } );
    }
}
