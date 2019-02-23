import java.sql.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

class Check{
    void initCheck(){
        Connection connection = new ConnectionDB().connect(); //подключаемся к БД и получаем connection
        Timer t = new Timer();
        UpdateDataBase updateDataBase = new UpdateDataBase(connection);
        CheckTickets checkTickets = new CheckTickets(connection);
        t.scheduleAtFixedRate(updateDataBase, 0, 20000);
        while(CheckTickets.resultSet == null){
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        t.scheduleAtFixedRate(checkTickets, 0, 60000 * 3);

    }
}



class UpdateDataBase extends TimerTask {
    private Connection connection = null;
    private Statement statement = null;
    private ResultSet resultSet = null;
    private String select = "SELECT id, \"from\", \"to\", \"date\", maxPrice, idUser FROM needtickets";

    UpdateDataBase(Connection connection){
        this.connection = connection;
    }

    @Override
    public void run() {
    //update data from DB
        try{
            statement = connection.createStatement();
            resultSet = statement.executeQuery(select);
            CheckTickets.resultSet = resultSet;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}





class CheckTickets extends TimerTask {

    static ResultSet resultSet = null;
    private Main main = new Main();
    private Bot bot = new Bot();
    private Connection connection = null;
    CheckTickets(Connection connection){
        this.connection = connection;
    }

    @Override
    public void run() {
        try{
            while (resultSet.next()) {
                String id = resultSet.getString("id");
                String from = resultSet.getString("from");
                String to = resultSet.getString("to");
                String date = resultSet.getString("date");
                String maxPrice = resultSet.getString("maxPrice");
                String idUser = resultSet.getString("idUser");
                System.out.println(id);

                ArrayList<String> response = main.main(from, to, date, maxPrice); //проверяем, появился ли билет

                if(response!=null){
                    //информируем юзера о новом билете и удаляем запись из БД
                    bot.sendMessage("Мы нашли для Вас подходящие билеты!", idUser, null);
                    bot.output(response, idUser, null, null);

                    Statement statement = connection.createStatement();
                    statement.executeUpdate("DELETE FROM needtickets  WHERE id = "+id+"");
                }
                Thread.sleep(500);
            }
        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
