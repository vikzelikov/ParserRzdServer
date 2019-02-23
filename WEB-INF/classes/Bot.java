import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


@WebServlet("/bot") //доступ к боту по данной ссылке

public class Bot extends HttpServlet {

    private final String confirmation = "7e3f5063"; //для связывания сервера с VK API
    private final String token = "fb01d9951e6c60223b60f6829f2e1bc95fc6698aca3e66269aef45bd76f6fa4e86df788b91897ed376875"; //токен доступа к VK API

    private Main main = new Main();
    private JSONObject objectJson = null;
    private Map<String, DataUsers> mapUsers = new HashMap<>();
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");
    {
        DATE_FORMAT.setLenient(true);
    }


    //buttons
    private String defaultBtn = null;
    private String yesOrNo = null;
    private String passStep = null;
    private String editAndBack = null;
    private String back = null;
    private String fromToDateMaxprice = null;
    {
        try {
            defaultBtn = "{\"one_time\":true,\"buttons\":[[{\"action\":{\"type\":\"text\",\"payload\":\"{\\\"button\\\":\\\"1\\\"}\",\"label\":\""+URLEncoder.encode(("Поиск билетов"), "UTF-8")+"\"},\"color\":\"primary\"},{\"action\":{\"type\":\"text\",\"payload\":\"{\\\"button\\\":\\\"2\\\"}\",\"label\":\""+URLEncoder.encode("Мои запросы", "UTF-8")+"\"},\"color\":\"primary\"}]]}";
            yesOrNo = "{\"one_time\":true,\"buttons\":[[{\"action\":{\"type\":\"text\",\"payload\":\"{\\\"button\\\":\\\"1\\\"}\",\"label\":\""+URLEncoder.encode("Да", "UTF-8")+"\"},\"color\":\"positive\"},{\"action\":{\"type\":\"text\",\"payload\":\"{\\\"button\\\":\\\"2\\\"}\",\"label\":\""+URLEncoder.encode("Нет", "UTF-8")+"\"},\"color\":\"default\"}]]}";
            passStep = "{\"one_time\":true,\"buttons\":[[{\"action\":{\"type\":\"text\",\"payload\":\"{\\\"button\\\":\\\"1\\\"}\",\"label\":\""+URLEncoder.encode("Пропустить", "UTF-8")+"\"},\"color\":\"primary\"}]]}";
            editAndBack = "{\"one_time\":true,\"buttons\":[[{\"action\":{\"type\":\"text\",\"payload\":\"{\\\"button\\\":\\\"1\\\"}\",\"label\":\""+URLEncoder.encode("Вернуться в меню", "UTF-8")+"\"},\"color\":\"default\"},{\"action\":{\"type\":\"text\",\"payload\":\"{\\\"button\\\":\\\"2\\\"}\",\"label\":\""+URLEncoder.encode("Редактировать", "UTF-8")+"\"},\"color\":\"primary\"}]]}";
            back = "{\"one_time\":false,\"buttons\":[[{\"action\":{\"type\":\"text\",\"payload\":\"{\\\"button\\\":\\\"1\\\"}\",\"label\":\""+URLEncoder.encode("Вернуться в меню", "UTF-8")+"\"},\"color\":\"default\"}]]}";
            fromToDateMaxprice = "{\"one_time\":true,\"buttons\":[[{\"action\":{\"type\":\"text\",\"payload\":\"{\\\"button\\\":\\\"1\\\"}\",\"label\":\""+URLEncoder.encode(("Откуда"), "UTF-8")+"\"},\"color\":\"primary\"},{\"action\":{\"type\":\"text\",\"payload\":\"{\\\"button\\\":\\\"2\\\"}\",\"label\":\""+URLEncoder.encode("Куда", "UTF-8")+"\"},\"color\":\"primary\"}],[{\"action\":{\"type\":\"text\",\"payload\":\"{\\\"button\\\":\\\"1\\\"}\",\"label\":\""+URLEncoder.encode(("Дата"), "UTF-8")+"\"},\"color\":\"primary\"},{\"action\":{\"type\":\"text\",\"payload\":\"{\\\"button\\\":\\\"2\\\"}\",\"label\":\""+URLEncoder.encode("Макс. цена", "UTF-8")+"\"},\"color\":\"primary\"}]]}";

        } catch (UnsupportedEncodingException ignored) {}
    }




    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        synchronized (this) { //синхронизируем обьект для многопоточного пользования
            //for searching
            String[] param = null;
            boolean searchNewTickets = false;
            boolean notTickets = false;
            ArrayList<String> dataTickets = null;

            //for editing
            java.sql.Connection connectionDB = null;
            ResultSet resultSet = null;
            StringBuilder dataQueries = null;
            int countQueries = 1;
            int numberTicketForEdit = 0;
            int stagesForEditing = -1; //variable for stages
            /*
             * 0 - стадия просмотра текущий запросов
             * 1 - стадия выбора билета для редактирования
             * 2 - стадия выбора параметра билета для редактирования
             * 10 - выбран параметр КУДА
             * 11 - выбран параметр ОТКУДА
             * 12 - выбран параметр ДАТА
             * 13 - выбран параметр MaxPrice
             * 20 - обновление ДБ и вывод уже отрекдактированного билета
             */


            DataUsers dataUsers = null;
            String idUser = null;
            String alertFromVkAPI = getBodyDecoder(req); //получаем уведомление от API VK


            try {
                objectJson = (JSONObject) new JSONParser().parse(alertFromVkAPI);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (String.valueOf(objectJson.get("type")).equals("confirmation")) {
                resp.getWriter().print(confirmation);
            }

            if (String.valueOf(objectJson.get("type")).equals("message_new")) {
                JSONObject object = (JSONObject) objectJson.get("object"); //если это новое сообщение
                String message = String.valueOf(object.get("text"));
                idUser = String.valueOf(object.get("from_id"));


                //получаем все стадии для пользователя по idUser
                if (mapUsers.containsKey(idUser)) {
                    dataUsers = mapUsers.get(idUser);

                    //for searching
                    param = dataUsers.getParam();
                    searchNewTickets = dataUsers.getSearchNewTickets();
                    notTickets = dataUsers.getNotTickets();
                    dataTickets = dataUsers.getDataTickets();

                    //for editing
                    connectionDB = dataUsers.getConnectionDB();
                    resultSet = dataUsers.getResultSet();
                    dataQueries = dataUsers.getDataQueries();
                    countQueries = dataUsers.getCountQueries();
                    numberTicketForEdit = dataUsers.getNumberTicketForEdit();
                    stagesForEditing = dataUsers.getStagesForEditing(); //variable for stages

                } else {
                    mapUsers.put(idUser, new DataUsers()); //если юзер пользуется ботом впервые
                }


                //FOR LOCAL TESTING ********************************
//        String message = getBodyDecoder(req);
//        message = URLDecoder.decode(message, "utf-8");
//        String[] arr = message.split("=");
//        message = arr[1];
//        arr = message.split("&");
//        message = arr[0];
//
//        String idUser = "161766924";
//
//        if(mapUsers.containsKey(idUser)){
//            dataUsers = mapUsers.get(idUser);
//
//            //for searching
//            param = dataUsers.getParam();
//            searchNewTickets = dataUsers.getSearchNewTickets();
//            notTickets = dataUsers.getNotTickets();
//            dataTickets = dataUsers.getDataTickets();
//
//            //for editing
//            connectionDB = dataUsers.getConnectionDB();
//            resultSet = dataUsers.getResultSet();
//            dataQueries = dataUsers.getDataQueries();
//            countQueries = dataUsers.getCountQueries();
//            numberTicketForEdit = dataUsers.getNumberTicketForEdit();
//            stagesForEditing = dataUsers.getStagesForEditing(); //variable for stages
//
//        }else{
//            mapUsers.put(idUser, new DataUsers());
//        }
                //FOR LOCAL TESTING *********************************


                switch (message.toUpperCase()) {

                    case ("ПОИСК БИЛЕТОВ"): {
                        if (!searchNewTickets) {
                            searchNewTickets = true;
                            param = new String[4];
                            sendMessage("Откуда:", idUser, back);
                        }
                        break;
                    }

                    case ("ВЕРНУТЬСЯ В МЕНЮ"): {
                        param = null;
                        searchNewTickets = false;
                        notTickets = false;
                        dataTickets = null;

                        connectionDB = null;
                        resultSet = null;
                        dataQueries = null;
                        countQueries = 1;
                        numberTicketForEdit = 0;
                        stagesForEditing = -1;

                        sendMessage("Вы вернулись в главное меню", idUser, defaultBtn);
                        break;
                    }


                    case ("МОИ ЗАПРОСЫ"): {
                        if (stagesForEditing == -1) {

                            //обнуляем данные
                            param = null;
                            searchNewTickets = false;
                            notTickets = false;
                            dataTickets = null;


                            connectionDB = new ConnectionDB().connect(); //соединяемся с БД
                            Statement statement = null;
                            String select = "SELECT id, \"from\", \"to\", \"date\", maxPrice FROM needtickets WHERE idUser = '" + idUser + "'";
                            dataQueries = new StringBuilder();

                            //получаем запросы из БД
                            try {
                                statement = connectionDB.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY); //чтобы можно было двигать указатель в ResultSet не только вперед
                                resultSet = statement.executeQuery(select);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }


                            //выводим запросы
                            try {
                                if (!resultSet.next()) { //проверили на пустоту
                                    throw new SQLException();
                                }
                                resultSet.beforeFirst(); //вернули указатель на начало
                                while (resultSet.next()) {
                                    String from = resultSet.getString("from");
                                    String to = resultSet.getString("to");
                                    String date = resultSet.getString("date");
                                    String maxPrice = resultSet.getString("maxPrice");
                                    if (maxPrice.equals("pass")) {
                                        maxPrice = "";
                                    } else {
                                        maxPrice = "\nЦена до: " + maxPrice;
                                    }

                                    dataQueries.append("Билет №").append(countQueries).append("\nОткуда: ").append(from).append("\nКуда: ").append(to).append("\nДата: ").append(date).append(maxPrice).append("\n\n");
                                    countQueries++;


                                }
                                sendMessage(dataQueries.toString(), idUser, editAndBack);
                                stagesForEditing = 0; //стадия МОИ ЗАПРОСЫ
                                resultSet.beforeFirst(); //вернули указатель на начальное положение
                            } catch (SQLException e) {
                                sendMessage("Запросов не найдено!", idUser, defaultBtn);
                            }
                        }


                        break;
                    }


                    default: {
                        //SEARCH TICKETS ********************
                        if (searchNewTickets) {
                            if (param[0] == null) {
                                if (main.getCity(message, true) != null) {
                                    param[0] = message; //откуда
                                    sendMessage("Куда:", idUser, back);
                                } else {
                                    sendMessage("Такой город не найден, попробуйте еще раз!", idUser, null);
                                    sendMessage("Откуда:", idUser, back);
                                }

                            } else if (param[1] == null) {
                                if (main.getCity(message, true) != null) {
                                    param[1] = message; //куда
                                    sendMessage("Дата:", idUser, back);
                                } else {
                                    sendMessage("Такой город не найден, попробуйте еще раз!", idUser, null);
                                    sendMessage("Куда:", idUser, back);
                                }

                            } else if (param[2] == null) {
                                if(isValidDate(message)){
                                    param[2] = message; //дата
                                    sendMessage("Максимальная цена билета:", idUser, passStep);
                                }else{
                                    sendMessage("Введите валидную дату! (Формат: DD.MM.YYYY)", idUser, null);
                                    sendMessage("Дата:", idUser, back);
                                }

                            } else if (param[3] == null) {
                                if (message.toUpperCase().equals("ПРОПУСТИТЬ")) { //если ПРОПУСТИТЬ, то ищем по трем параметрам
                                    param[3] = "pass";
                                } else { //если не ПРОПУСТИТЬ, то проверяем корректность числа и ищем по четырем параметрам
                                    if (checkDigit(message, -1)) {
                                        param[3] = message;
                                    } else {
                                        sendMessage("Введите корректное число!", idUser, null);
                                        sendMessage("Максимальная цена билета:", idUser, passStep);
                                    }
                                }

                            }

                            //если все параметры к поиску введены, ищем билеты и посылаем сообщение
                            if (param[3] != null) {
                                if (!notTickets) {
                                    //ищем по параметрам
                                    sendMessage("Поиск...", idUser, back);
                                    dataTickets = main.main(param[0], param[1], param[2], param[3]); //получаем билеты
                                }

                                if (dataTickets == null) {
                                    if (!notTickets) {
                                        sendMessage("Билетов нет!\n", idUser, null);
                                        sendMessage("Мы можем написать Вам сообщение в VK, как только билеты появятся\nВы согласны?", idUser, yesOrNo);
                                        //отправили две кнопки: Да или Нет
                                        notTickets = true;
                                    } else {
                                        switch (message.toUpperCase()) {
                                            case "ДА":
                                                main.writeDB(param[0], param[1], param[2], param[3], idUser); //записали в базу данных

                                                sendMessage("Спасибо! Мы Вас оповестим!", idUser, defaultBtn);

                                                //обнуляем данные
                                                param = null;
                                                searchNewTickets = false;
                                                notTickets = false;
                                                dataTickets = null;

                                                break;
                                            case "НЕТ":
                                                sendMessage("Вы отказались", idUser, defaultBtn);

                                                //обнуляем данные
                                                param = null;
                                                searchNewTickets = false;
                                                notTickets = false;
                                                dataTickets = null;

                                                break;
                                            default:
                                                sendMessage("Введите корректную команду!\nВы согласны?", idUser, yesOrNo);
                                                break;
                                        }


                                    }

                                } else {
                                    //output data
                                    output(dataTickets, idUser, defaultBtn, null);

                                    //обнуляем данные
                                    param = null;
                                    searchNewTickets = false;
                                    notTickets = false;
                                    dataTickets = null;

                                }
                            }
                            //SEARCH TICKETS ********************

                        } else if (!searchNewTickets && stagesForEditing == -1) {
                            sendMessage("Неверная команда", idUser, defaultBtn);
                            //вывод всех команд
                            sendMessage("Поддерживаемые команды:\n- Поиск билетов\n- Вернуться в меню\n- Мои запросы", idUser, defaultBtn);
                        }


                        //EDIT TICKETS *************
                        if (stagesForEditing > -1) {
                            if (message.toUpperCase().equals("РЕДАКТИРОВАТЬ") || stagesForEditing > 0) {
                                if (stagesForEditing == 0) {
                                    sendMessage("Отправьте номер билета для редактирования", idUser, back);
                                    stagesForEditing = 1; // стадия ВЫБОР БИЛЕТА ДЛЯ РЕДАКТИРОВАНИЯ
                                } else {
                                    if (checkDigit(message, countQueries) || stagesForEditing > 1) {
                                        if (stagesForEditing == 1) {
                                            try {
                                                numberTicketForEdit = Integer.parseInt(message); //получили номер билета для редактирования
                                                resultSet.absolute(numberTicketForEdit); //поставили указатель на нужный нам билет
                                                dataQueries = new StringBuilder();

                                                String from = resultSet.getString("from");
                                                String to = resultSet.getString("to");
                                                String date = resultSet.getString("date");
                                                String maxPrice = resultSet.getString("maxPrice");
                                                if (maxPrice.equals("pass")) {
                                                    maxPrice = "";
                                                } else {
                                                    maxPrice = "\nЦена до: " + maxPrice;
                                                }

                                                dataQueries.append("Билет №").append(numberTicketForEdit).append("\nОткуда: ").append(from).append("\nКуда: ").append(to).append("\nДата: ").append(date).append(maxPrice).append("\n\n");
                                                sendMessage(dataQueries.toString(), idUser, null);
                                                sendMessage("Какой параметр желаете изменить?", idUser, fromToDateMaxprice);
                                                stagesForEditing = 2; //стадия ВЫБОР ПАРАМЕТРА ДЛЯ РЕДАКТИРОВАНИЯ
                                            } catch (SQLException e) {
                                                //ignore
                                            }
                                        } else {
                                            if (stagesForEditing == 2) {
                                                switch (message.toUpperCase()) {
                                                    case ("ОТКУДА"): {
                                                        sendMessage("Введите новое значение параметра: ", idUser, back);
                                                        stagesForEditing = 10;
                                                        break;
                                                    }
                                                    case ("КУДА"): {
                                                        sendMessage("Введите новое значение параметра: ", idUser, back);
                                                        stagesForEditing = 11;
                                                        break;
                                                    }
                                                    case ("ДАТА"): {
                                                        sendMessage("Введите новое значение параметра: ", idUser, back);
                                                        stagesForEditing = 12;
                                                        break;
                                                    }
                                                    case ("МАКС. ЦЕНА"): {
                                                        sendMessage("Введите новое значение параметра: ", idUser, back);
                                                        stagesForEditing = 13;
                                                        break;
                                                    }
                                                    default: {
                                                        sendMessage("Введите корректный параметр: откуда, куда, дата, макс. цена", idUser, fromToDateMaxprice);
                                                        break;
                                                    }
                                                }
                                            } else {

                                                Statement statement = null;
                                                String id = null;
                                                try {
                                                    statement = connectionDB.createStatement();
                                                    id = resultSet.getString("id");
                                                    if (stagesForEditing == 10) {
                                                        if (main.getCity(message, true) != null) {
                                                            //update
                                                            statement.executeUpdate("UPDATE needtickets SET \"from\" = '" + message.toUpperCase() + "' WHERE id =" + id + "");
                                                            stagesForEditing = 20; //стадия ГОТОВ К ВЫВОДУ
                                                        } else {
                                                            sendMessage("Введенный Вами новый город не найден! Повторите попытку!\nВведите новое значение параметра: ", idUser, back);
                                                        }
                                                    }

                                                    if (stagesForEditing == 11) {
                                                        if (main.getCity(message, true) != null) {
                                                            //update
                                                            statement.executeUpdate("UPDATE needtickets SET \"to\" = '" + message.toUpperCase() + "' WHERE id =" + id + "");
                                                            stagesForEditing = 20; //стадия ГОТОВ К ВЫВОДУ
                                                        } else {
                                                            sendMessage("Введенный Вами новый город не найден! Повторите попытку!\nВведите новое значение параметра: ", idUser, back);
                                                        }
                                                    }

                                                    if (stagesForEditing == 12) {
                                                        if(isValidDate(message)){
                                                            //update
                                                            statement.executeUpdate("UPDATE needtickets SET \"date\" = '" + message + "' WHERE id =" + id + "");
                                                            stagesForEditing = 20; //стадия ГОТОВ К ВЫВОДУ
                                                        }else{
                                                            sendMessage("Введите валидную дату! (Формат: DD.MM.YYYY)\nВведите новое значение параметра: ", idUser, back);
                                                        }

                                                    }

                                                    if (stagesForEditing == 13) {
                                                        if (checkDigit(message, -1)) {
                                                            //update
                                                            statement.executeUpdate("UPDATE needtickets SET maxprice = " + message + " WHERE id =" + id + "");
                                                            stagesForEditing = 20; //стадия ГОТОВ К ВЫВОДУ
                                                        } else {
                                                            sendMessage("Введите корректное число!", idUser, null);
                                                            sendMessage("Максимальная цена билета:", idUser, back);
                                                        }
                                                    }


                                                    //вывод уже отредактрированного билета
                                                    if (stagesForEditing == 20) {
                                                        sendMessage("Параметры сохранены!", idUser, null);
                                                        String select = "SELECT \"from\", \"to\", \"date\", maxPrice FROM needtickets WHERE id = " + id + "";
                                                        dataQueries = new StringBuilder();

                                                        //получаем запросы из БД
                                                        try {
                                                            statement = connectionDB.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY); //чтобы можно было двигать указатель в ResultSet не только вперед
                                                            resultSet = statement.executeQuery(select);
                                                            resultSet.next();
                                                        } catch (SQLException e) {
                                                            e.printStackTrace();
                                                        }


                                                        String from = resultSet.getString("from");
                                                        String to = resultSet.getString("to");
                                                        String date = resultSet.getString("date");
                                                        String maxPrice = resultSet.getString("maxPrice");
                                                        if (maxPrice.equals("pass")) {
                                                            maxPrice = "";
                                                        } else {
                                                            maxPrice = "\nЦена до: " + maxPrice;
                                                        }

                                                        dataQueries.append("Билет №").append(numberTicketForEdit).append("\nОткуда: ").append(from).append("\nКуда: ").append(to).append("\nДата: ").append(date).append(maxPrice).append("\n\n");
                                                        sendMessage(dataQueries.toString(), idUser, null);

                                                        //обнуялем все данные и флаги
                                                        connectionDB = null;
                                                        resultSet = null;
                                                        dataQueries = null;
                                                        countQueries = 1;
                                                        numberTicketForEdit = 0;
                                                        stagesForEditing = -1;

                                                        sendMessage("Вы вернулись в главное меню", idUser, defaultBtn);
                                                    }

                                                } catch (SQLException e) {
                                                    e.printStackTrace();
                                                }

                                            }
                                        }
                                    } else {
                                        sendMessage("Введите корректный номер билета!", idUser, back);
                                    }
                                }
                            } else {
                                sendMessage("Введите корректную команду!", idUser, editAndBack);
                            }

                        }
                        //EDIT TICKETS *************


                        break;
                    }
                }
                resp.getWriter().print("ok");

            }


            if (dataUsers != null) {
                dataUsers.setParam(param);
                dataUsers.setSearchNewTickets(searchNewTickets);
                dataUsers.setNotTickets(notTickets);
                dataUsers.setDataTickets(dataTickets);

                dataUsers.setConnectionDB(connectionDB);
                dataUsers.setResultSet(resultSet);
                dataUsers.setDataQueries(dataQueries);
                dataUsers.setCountQueries(countQueries);
                dataUsers.setNumberTicketForEdit(numberTicketForEdit);
                dataUsers.setStagesForEditing(stagesForEditing);

                mapUsers.put(idUser, dataUsers); //записали в обьект все стадии
            }
        }
}






    String getBodyDecoder(HttpServletRequest request) throws IOException {
        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            }
        } finally {
            bufferedReader.close();
        }
        body = stringBuilder.toString();
        return body;
    }


    private boolean checkDigit(String digit, int countQueries){
        if(countQueries != -1){
            try {
                if(Integer.parseInt(digit) >= 1 && Integer.parseInt(digit) <= countQueries - 1){
                    return true;
                }else throw new NumberFormatException();
            }catch (NumberFormatException ex){
                return  false;
            }
        }else {
            try {
                if(Integer.parseInt(digit) >= 1){
                    return true;
                }else throw new NumberFormatException();
            }catch (NumberFormatException ex){
                return  false;
            }
        }
    }


    void output(ArrayList<String> data, String idUser, String keyboard, HttpServletResponse resp){
        //оправляем данные пользователю в ВК
        StringBuilder informationTickets = new StringBuilder();

        int i = 0;
        int j = 0;
        while(i < data.size()){
            if(!data.get(i).equals("\n")){
                informationTickets.append(data.get(i)).append("\n");
            }

            try {
                if (data.get(i + 1).equals("\n")) {
                    if (j == 2) {
                        if(resp != null){
                            resp.getWriter().print(informationTickets.append("\n\n").toString());
                        }else {
                            sendMessage(informationTickets.toString(), idUser, keyboard);
                        }
                        informationTickets = new StringBuilder();
                        j = 0;
                    } else {
                        informationTickets.append("\n\n");
                        j++;
                    }
                }
                i++;
            }catch (Exception ex){
                if(resp != null){
                    try {
                        resp.getWriter().print(informationTickets.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else {
                    sendMessage(informationTickets.toString(), idUser, keyboard); //выводим остатавшиеся билеты (остаток от трех)
                }
                break;
            }
        }
    }


    void sendMessage(String message, String idUser, String keyboard){
        String link = null;
        HttpURLConnection connection = null;
        Random random = new Random();
        try{
            link = "https://api.vk.com/method/messages.send?message="+URLEncoder.encode((message), "UTF-8")+"&user_id="+idUser+"&access_token="+token+"&random_id="+random.nextInt(999999999)+"&v=5.92";
            if(keyboard != null){
                link += ("&keyboard="+keyboard);
            }

            connection = (HttpURLConnection) new URL(link).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            connection.getInputStream();
        }catch (Exception ex){
            ex.printStackTrace();
        }finally {
            connection.disconnect();
        }


    }


    private boolean isValidDate(String date){
        try {
            return DATE_FORMAT.format(DATE_FORMAT.parse(date)).equals(date);
        }catch (Exception ex){
            return false;
        }
    }

}
