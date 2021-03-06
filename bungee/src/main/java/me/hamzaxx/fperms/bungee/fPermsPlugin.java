package me.hamzaxx.fperms.bungee;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import me.hamzaxx.fperms.bungee.commands.fPermsCommand;
import me.hamzaxx.fperms.bungee.data.DataSource;
import me.hamzaxx.fperms.bungee.data.redis.RedisDataSource;
import me.hamzaxx.fperms.bungee.listeners.LoginListener;
import me.hamzaxx.fperms.bungee.listeners.PermissionListener;
import me.hamzaxx.fperms.bungee.listeners.ServerListener;
import me.hamzaxx.fperms.bungee.netty.ServerHandler;
import me.hamzaxx.fperms.common.netty.Change;
import me.hamzaxx.fperms.common.netty.ServerBye;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class fPermsPlugin extends Plugin
{

    private DataSource dataSource;
    private Config config;

    private Channel channel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ConcurrentMap<String, Channel> channels = new ConcurrentHashMap<>();

    private Gson exclusionaryGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    private Gson gson = new Gson();

    @Override
    public void onEnable()
    {
        saveDefaultConfig();
        config = new Config( this );
        dataSource = new RedisDataSource( this );
        getProxy().getScheduler().schedule( this, this::setupServer, 2, TimeUnit.SECONDS );
        getProxy().getPluginManager().registerCommand( this, new fPermsCommand( this ) );
        //getProxy().getPluginManager().registerCommand( this, new CommandExecutor( "fPerms", fPermsCommand.class ) );
        registerListeners();
    }

    @Override
    public void onDisable()
    {
        kill();
    }


    private void saveDefaultConfig()
    {
        if ( !getDataFolder().exists() )
        {
            if ( getDataFolder().mkdir() )
            {
                getLogger().info( "Config folder created!" );
            }
        }

        File file = new File( getDataFolder(), "bungeeconfig.yml" );

        if ( !file.exists() )
        {
            try
            {
                Files.copy( getResourceAsStream( "bungeeconfig.yml" ), file.toPath() );
            } catch ( IOException ex )
            {
                getProxy().getLogger().severe( ex.getMessage() );
            }
        }
    }

    private void kill()
    {
        getDataSource().close();
        try
        {
            getChannels().values().forEach( channel -> {
                try
                {
                    channel.writeAndFlush( new String[]{ "serverBye",
                            getGson().toJson( new ServerBye( "BungeeCord shutdown" ) ) } ).await();
                } catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
            } );
        } finally
        {
            channel.closeFuture();
            channel.close();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private void registerListeners()
    {
        Stream.of( new ServerListener( this ), new PermissionListener( this ), new LoginListener( this ) ).forEach( listener ->
                getProxy().getPluginManager().registerListener( this, listener ) );
    }

    private void setupServer()
    {
        bossGroup = new NioEventLoopGroup( 1 );
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        ServerHandler serverHandler = new ServerHandler( this );
        b.group( bossGroup, workerGroup )
                .channel( NioServerSocketChannel.class )
                .childHandler( new ChannelInitializer<SocketChannel>()
                {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception
                    {
                        socketChannel.pipeline().addLast( new ObjectDecoder(
                                        ClassResolvers.cacheDisabled( ClassLoader.getSystemClassLoader() ) ),
                                new ObjectEncoder(), serverHandler );
                    }
                } );
        b.localAddress( "0.0.0.0", getConfig().getPort() );
        try
        {
            channel = b.bind( getConfig().getPort() ).sync().channel();
        } catch ( InterruptedException e )
        {
            kill();
        }
    }


    public void sendToServer(ServerInfo server, Change change)
    {
        getChannels().get( server.getName() )
                .writeAndFlush( new String[]{ "change", getExclusionaryGson().toJson( change ), change.getData() } );
    }

    public void sendToServer(Server server, Change change)
    {
        getChannels().get( server.getInfo().getName() )
                .writeAndFlush( new String[]{ "change", getExclusionaryGson().toJson( change ), change.getData() } );
    }

    public void sentToAll(Change change)
    {
        getChannels().values().forEach( channel ->
                channel.writeAndFlush( new String[]{ "change", getExclusionaryGson().toJson( change ), change.getData() } ) );
    }

    public Config getConfig()
    {
        return config;
    }

    public ConcurrentMap<String, Channel> getChannels()
    {
        return channels;
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public Gson getExclusionaryGson()
    {
        return exclusionaryGson;
    }

    public Gson getGson()
    {
        return gson;
    }
}
