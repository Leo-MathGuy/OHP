package omega.database;

import arc.Core;
import arc.util.Log;
import arc.util.Strings;

import mindustry.gen.Player;

import org.json.simple.JSONObject;

import java.sql.*;

import static omega.utils.Logger.*;


public class dataHandler {
    static final String DB_URL = "jdbc:mariadb://localhost:3306/omegaMindustry";

    //  Database credentials
    static final String USER = "";
    static final String PASS = "";

    public static void Init() {
        Connection conn = null;
        Statement stmt;
        try {
            Class.forName("org.mariadb.jdbc.Driver");

            Log.warn("Connecting to database...");
            try{
                conn = DriverManager.getConnection(DB_URL, USER, PASS);
            }
            catch (SQLException e) {
                Log.err("Connection failed");
                Log.err(e);
                Core.app.exit();
            }
            finally {
                Log.info("Connected database successfully...");
            }

            Log.warn("Creating table in database if it doesn't exist yet...");
            assert conn != null;
            stmt = conn.createStatement();

            String sql = "CREATE TABLE IF NOT EXISTS servers "
                    + "(id INTEGER AUTO_INCREMENT not NULL,"
                    + " username VARCHAR(255) not NULL,"
                    + " password VARCHAR(255) not NULL,"
                    + " ingameName VARCHAR(255), "
                    + " banned BOOLEAN not NULL,"
                    + " uuid VARCHAR(255), "
                    + " ip VARCHAR(255), "
                    + " rank VARCHAR(255), "
                    + " xp INTEGER, "
                    + " joindate VARCHAR(255), "
                    + " playtime INTEGER, "
                    + " discord_id VARCHAR(255), "
                    + " PRIMARY KEY ( id ))";

            stmt.executeUpdate(sql);
            Log.info("Success!");
            stmt.close();
            conn.close();
        } catch (SQLException se) {
            Log.err("<SQL Exception>", se);
        } catch (Exception e) {
            Log.err("<Database> Class not found:", e);
        }
    }

    public static boolean isLinked(Player p) {
        Connection conn = null;
        PreparedStatement sql;

        try {
            Class.forName("org.mariadb.jdbc.Driver");

            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            sql = conn.prepareStatement("SELECT * FROM servers WHERE uuid = ?");
            sql.setString(1, p.uuid());
            ResultSet rs = sql.executeQuery();

            if (rs.next()) {
                return true;
            }
            rs.close();
            conn.close();
        } catch (SQLException se) {
            Log.err("<SQL Exception>", se);
        } catch (Exception e) {
            Log.err("<Database> Class not found:", e);
        }
        return false;
    }

    public static void set_rank(Player player, String rank) {
        Connection conn = null;
        PreparedStatement sql;

        try {
            Class.forName("org.mariadb.jdbc.Driver");

            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            sql = conn.prepareStatement("UPDATE servers SET rank = ? WHERE uuid = ?");
            sql.setString(1, rank);
            sql.setString(2, player.uuid());
            sql.executeUpdate();

            sql.close();
            conn.close();
        } catch (SQLException se) {
            Log.err("<SQL Exception>", se);
        } catch (Exception e) {
            Log.err("<Database> Class not found:", e);
        }
    }

    public boolean isBanned(Player player) {
        Connection conn = null;
        PreparedStatement sql;

        try {
            Class.forName("org.mariadb.jdbc.Driver");

            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            sql = conn.prepareStatement("SELECT * FROM servers WHERE uuid = ?");
            sql.setString(1, player.uuid());
            ResultSet rs = sql.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("banned");
            }
            rs.close();
            conn.close();
        } catch (SQLException se) {
            Log.err("<SQL Exception>", se);
        } catch (Exception e) {
            Log.err("<Database> Class not found:", e);
        }
        return false;
    }
    public boolean isLinkedToDiscord(Player p) {
        Connection conn = null;
        PreparedStatement sql;

        try {
            Class.forName("org.mariadb.jdbc.Driver");

            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            sql = conn.prepareStatement("SELECT * FROM servers WHERE uuid = ?");
            sql.setString(1, p.uuid());
            ResultSet rs = sql.executeQuery();

            if (rs.next()) {
                return !rs.getString("discord_id").equals("0");
            }
            rs.close();
            conn.close();
        } catch (SQLException se) {
            Log.err("<SQL Exception>", se);
        } catch (Exception e) {
            Log.err("<Database> Class not found:", e);
        }
        return false;
    }
    public void send_registry(Player p, String role, String username, String password){
        long time = System.currentTimeMillis() / 1000;

        PreparedStatement sql;
        Connection conn;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            sql = conn.prepareStatement("INSERT INTO servers(ingameName, username, password, uuid, ip, banned, rank, xp, joindate, playtime, discord_id) VALUES (?,?,?,?,?,?,?,?,?,?,?)");
            sql.setString(1, p.name)
               .setString(2, username)
               .setString(3, password)
               .setString(4, p.uuid())
               .setString(5, p.con.address)
               .setBoolean(6, false)
               .setString(7, role)
               .setInt(8, 0)
               .setString(9, Long.toString(time))
               .setInt(10, 0)
               .setString(11, "0")
               .executeUpdate();
            Log.info("Successfully Registered!" + Strings.stripColors(p.name));
            sql.close();
            conn.close();
        } catch (SQLException | ClassNotFoundException e) {
            Log.err(e);
        }
    }
    public String check_registry(Player p, String username, String password) {
        Connection conn = null;
        PreparedStatement sql;
        String pass = null;
        String rank = null;
        String usr = null;
        String joindate = null;
        String playtime = null;
        String xp = null;
        String yes = null;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            sql = conn.prepareStatement("SELECT * FROM servers WHERE username = ? AND password = ? OR uuid = ?");
            sql.setString(1, username);
            sql.setString(2, password);
            sql.setString(3, p.uuid());
            ResultSet rs = sql.executeQuery();
            while (rs.next()) {
                usr = rs.getString("username");
                pass = rs.getString("password");
                rank = rs.getString("rank");
                joindate = rs.getString("joindate");
                playtime = rs.getString("playtime");
                xp = rs.getString("xp");

                JSONObject jo = new JSONObject();
                jo.put("username", usr);
                jo.put("password", pass);
                jo.put("rank", rank);
                jo.put("joindate", joindate);
                jo.put("playtime", playtime);
                jo.put("xp", xp);
                yes = jo.toJSONString();
            }
            sql.close();
            rs.close();
            conn.close();
        } catch (SQLException e) {
            Log.err(e);
        } catch (ClassNotFoundException e) {
            Log.err(e);
        }
        return yes;
    }

    public String update_registry(Player p, String username, String password) {
        Connection conn = null;
        PreparedStatement sql;
        String pass = null;
        String rank = null;
        String usr = null;
        String joindate = null;
        String playtime = null;
        String xp = null;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            sql = conn.prepareStatement("UPDATE servers SET ip = ?, ingameName = ?, uuid = ? WHERE uuid = ? OR ip = ? OR username = ? OR password = ?");
            sql.setString(1, p.con.address);
            sql.setString(2, p.name);
            sql.setString(3, p.uuid());
            sql.setString(4, p.uuid());
            sql.setString(5, p.con.address);
            sql.setString(6, username);
            sql.setString(7, password);
            sql.executeUpdate();
            sql.close();
            conn.close();
        } catch (SQLException e) {
            Log.err(e);
            return "error";
        } catch (ClassNotFoundException e) {
            Log.err(e);
            return "error";
        }
        return "done";
    }
    public boolean check_regist(Player p) {
        Connection conn = null;
        PreparedStatement sql;
        String pass = null;
        String rank = null;
        String usr = null;
        String joindate = null;
        String playtime = null;
        String xp = null;
        String yes = null;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            sql = conn.prepareStatement("SELECT * FROM servers WHERE uuid = ?");
            sql.setString(1, p.uuid());
            ResultSet rs = sql.executeQuery();
            while (rs.next()) {
                usr = rs.getString("username");
                pass = rs.getString("password");
                rank = rs.getString("rank");
                joindate = rs.getString("joindate");
                playtime = rs.getString("playtime");
                xp = rs.getString("xp");

                JSONObject jo = new JSONObject();
                jo.put("username", usr);
                jo.put("password", pass);
                jo.put("rank", rank);
                jo.put("joindate", joindate);
                jo.put("playtime", playtime);
                jo.put("xp", xp);
                yes = jo.toJSONString();
            }
            sql.close();
            rs.close();
            conn.close();
        } catch (SQLException e) {
            Log.err(e);
        } catch (ClassNotFoundException e) {
            Log.err(e);
        }
        if(yes == null) {
            return false;
        } else {
            return true;
        }
    }

    public String check_xp(Player p) {
        PreparedStatement sql;
        Connection conn;
        String xp = null;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            sql = conn.prepareStatement("SELECT * FROM servers WHERE ingameName = ? AND uuid = ?");
            sql.setString(1, p.name);
            sql.setString(2, p.uuid());
            ResultSet rs = sql.executeQuery();
            while (rs.next()) {
                xp = rs.getString("xp");
            }
            sql.close();
            rs.close();
            conn.close();
        } catch (SQLException e) {
            Log.err(e);
        } catch (ClassNotFoundException e) {
            Log.err(e);
        }
        if(xp == null){
            return "0";
        }
        else{
            return xp;
        }
    }

    public int check_playtime(Player p) {
        PreparedStatement sql;
        Connection conn;
        Integer playtime = null;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            sql = conn.prepareStatement("SELECT * FROM servers WHERE ingameName = ? AND uuid = ?");
            sql.setString(1, p.name);
            sql.setString(2, p.uuid());
            ResultSet rs = sql.executeQuery();
            while (rs.next()) {
                playtime = rs.getInt("playtime");
            }
            sql.close();
            rs.close();
            conn.close();
        } catch (SQLException e) {
            Log.err(e);
        } catch (ClassNotFoundException e) {
            Log.err(e);
        }
        if(playtime == null){
            return 0;
        }
        else{
            return playtime;
        }
    }

    public String update_xp(Player p, int xp) {
        PreparedStatement sql;
        Connection conn;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            sql = conn.prepareStatement("UPDATE mindustry.servers SET xp = ? WHERE ingameName = ? AND uuid = ?");
            sql.setInt(1, xp);
            sql.setString(2, p.name);
            sql.setString(3, p.uuid());
            sql.executeUpdate();
            sql.close();
            conn.close();
        } catch (SQLException e) {
            Log.err(e);
        } catch (ClassNotFoundException e) {
            Log.err(e);
        }

        return null;

    }
    public String update_playtime(Player p, int playtime) throws SQLException {
        PreparedStatement sql;
        Connection conn;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            sql = conn.prepareStatement("UPDATE mindustry.servers SET playtime = ? WHERE ingameName = ? AND uuid = ?");
            sql.setInt(1, playtime);
            sql.setString(2, p.name);
            sql.setString(3, p.uuid());
            sql.executeUpdate();
            sql.close();
            conn.close();
        } catch (ClassNotFoundException e) {
            Log.err(e);
        }

        return null;

    }

    public String update_rank(String uuid, String rank) {
        PreparedStatement sql;
        Connection conn;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            sql = conn.prepareStatement("UPDATE mindustry.servers SET rank = ? WHERE uuid = ?");
            sql.setString(1, rank);
            sql.setString(2, uuid);
            sql.executeUpdate();
            sql.close();
            conn.close();
        } catch (SQLException e) {
            Log.err(e);
        } catch (ClassNotFoundException e) {
            Log.err(e);
        }

        return null;

    }

    public static String check_rank(Player p) {
        PreparedStatement sql;
        Connection conn;
        String rank = null;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            sql = conn.prepareStatement("SELECT * FROM servers WHERE ingameName = ? AND uuid = ?");
            sql.setString(1, p.name);
            sql.setString(2, p.uuid());
            ResultSet rs = sql.executeQuery();
            while (rs.next()) {
                rank = rs.getString("rank");
            }
            sql.close();
            rs.close();
            conn.close();
        } catch (SQLException | ClassNotFoundException e) {
            Log.err(e);
        }
        if(rank == null){
            return "player";
        }
        else{
            return rank;
        }
    }

    public static String set_dc(String dc_id, String uuid) {
        PreparedStatement sql;
        Connection conn;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            sql = conn.prepareStatement("UPDATE mindustry.servers SET discord_id = ? WHERE uuid = ?");
            sql.setString(1, dc_id);
            sql.setString(2, uuid);
            sql.executeUpdate();
            sql.close();
            conn.close();
        } catch (SQLException e) {
            discLogErr(e);
        } catch (ClassNotFoundException e) {
            discLogErr(e);
        }

        return null;

    }
}