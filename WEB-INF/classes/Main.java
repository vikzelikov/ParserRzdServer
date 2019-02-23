import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.net.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;





@WebServlet("/main")
public class Main extends HttpServlet {



static {
    new Check().initCheck(); //запуск автоматизации и базы данных
}


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setCharacterEncoding("Cp1251");

        if(req.getParameter("from") != null && req.getParameter("to") != null){
            String fromCity = req.getParameter("from");
            String toCity = req.getParameter("to");
            String date = req.getParameter("date");
            String maxPrice = req.getParameter("maxprice");
            String idUser = req.getParameter("vk");

            if(maxPrice == null)
                maxPrice = "pass";


            ArrayList<String> response = main(fromCity, toCity, date, maxPrice);
            if(response==null){
                if(idUser != null) {
                    //запись в бд так как необходимый билет не найден
                    resp.getWriter().print("Поездов по выбранным параметрам не найдено! Мы уведомим Вас, как только найдем подходящие билеты!");
                    writeDB(fromCity, toCity, date, maxPrice, idUser);
                }else{
                    resp.getWriter().print("Поездов по выбранным параметрам не найдено!");
                }
            }else {
                new Bot().output(response, null, null, resp);
            }
        }


    }








    ArrayList<String> main(String fromCity, String toCity, String date, String maximPrice){
        fromCity = fromCity.toUpperCase();
        toCity = toCity.toUpperCase();
        int maxPrice = 999999;
        if(!maximPrice.equals("pass")) {
            try {
                maxPrice = Integer.valueOf(maximPrice);
            }catch (NumberFormatException ex){
                maxPrice = 999999;
            }
        }


        //первый запрос, получаем rid
        Connection.Response res = null;
        Document documentFirst = null;
        String jsonStringDocumentFirst = null;
        Object objectJsonSecondDocument = null;
        ArrayList<String> data = null;
        String jsonData = null;
        String linkFirst = "http://pass.rzd.ru/timetable/public/ru?STRUCTURE_ID=735&layer_id=5371&dir=0&tfl=3&checkSeats=0&st0=" + fromCity + "&code0=" + getCity(fromCity, true) + "&st1=" + toCity + "&code1=" + getCity(toCity, true) + "&dt0=" + date + "";
        try {
            res = Jsoup.connect(linkFirst).method(Connection.Method.GET).execute();
            documentFirst = res.parse();
            jsonStringDocumentFirst = documentFirst.body().text();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //достаем из первого запроса rid и КУКИС
        Object obj = null;
        String rid = null;
        String sessionId = null;
        try {
            obj = new JSONParser().parse(jsonStringDocumentFirst);
            JSONObject ridJSON = (JSONObject) obj;
            rid = String.valueOf(ridJSON.get("rid")); //уникальный ключ доступа REQUEST_ID
            sessionId = res.cookie("JSESSIONID"); //достаем cookie

        } catch (ParseException e) {
            e.printStackTrace();
        }

        //второй запрос, получаем данные
        String jsonStringDocumentSecond = null;
        String linkSecond = "https://pass.rzd.ru/timetable/public/ru?layer_id=5371&rid=" + rid + "";
        Document documentSecond = null;
        try {
            String result = "";
            while(!result.equals("OK")) {
                documentSecond = Jsoup.connect(linkSecond).cookie("JSESSIONID", sessionId).get();
                jsonStringDocumentSecond = documentSecond.body().text();

                Object object = new JSONParser().parse(jsonStringDocumentSecond);
                JSONObject getObjectJson = (JSONObject) object;
                result = (String)getObjectJson.get("result");
                if (result.equals("Error")) {
                    jsonStringDocumentSecond = null;
                    break;
                }
                Thread.sleep(300);
            }
        } catch (ParseException | IOException | InterruptedException e) {
            e.printStackTrace();
        }


        jsonData = jsonStringDocumentSecond; // все данные о билетах в JSON



        //ПАРСИМ JSON
        //достаем из ответа второго запроса нужные данные(билеты, время отправления и тд)
        if(jsonData != null) {
            data = new ArrayList<>();
            try {
                objectJsonSecondDocument = new JSONParser().parse(jsonData);
                JSONObject getObjectJson = (JSONObject) objectJsonSecondDocument;
                JSONArray tp = (JSONArray) getObjectJson.get("tp");
                Iterator tpIter = tp.iterator();

                while (tpIter.hasNext()) {
                    JSONObject tpObj = (JSONObject) tpIter.next();
                    JSONArray list = (JSONArray) tpObj.get("list");
                    if (list.isEmpty()) {
                        throw new NullPointerException();
                    }
                    Iterator listIter = list.iterator();

                    int countTrains = 1;
                    while (listIter.hasNext()) {
                        JSONObject listObj = (JSONObject) listIter.next();
                        JSONArray cars = (JSONArray) listObj.get("cars");
                        Iterator carsIter = cars.iterator();

                        boolean start = false;

                        while (carsIter.hasNext()) {

                                JSONObject carsObj = (JSONObject) carsIter.next();
                                if (carsObj.get("typeLoc").equals("Багажное купе")) continue;


                                if (Integer.parseInt((String)carsObj.get("tariff")) <= maxPrice) {
                                    if (!start) {
                                        data.add("ПОЕЗД " + countTrains + ":");
                                        data.add("Отправление: " + listObj.get("time0") + " " + listObj.get("date0"));
                                        data.add("Прибытие: " + listObj.get("time1") + " " + listObj.get("date1"));
                                        data.add(listObj.get("station0") + " - " + listObj.get("station1"));
                                        data.add("Осталось свободных мест: ");
                                        countTrains++;
                                        start = true;
                                    }
                                    data.add(carsObj.get("typeLoc") + ": " + carsObj.get("freeSeats") + " (от " + carsObj.get("tariff") + " руб.)");
                                }

                        }
                        if (start)
                            data.add("\n");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                data = null; // т.е. нет билетов
            }
        }



            if (data != null && data.isEmpty()) {
                data = null; //если билетов по выбранным параметрам нет
            }


        return data;
    }



    String getCity(String city, boolean getCode) {

        String cityRu = city.toUpperCase();
        String cityUnicode = null;
        try {
            cityUnicode = URLEncoder.encode(cityRu.toUpperCase(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }



        URL url = null;
        HttpURLConnection connection = null;
        String line = null;
        String jsonDataCity = null;
        StringBuilder sb = new StringBuilder();

        try {
            //получаем коды станций по названию города
            url = new URL("http://www.rzd.ru/suggester?compactMode=y&stationNamePart=" + cityUnicode + "&lang=ru");
            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(6000);
            connection.setConnectTimeout(6000);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");


            //приводим в формат JSON
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            jsonDataCity = sb.toString(); //множество городов
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }

        if(getCode){
            return getCode(jsonDataCity, cityRu); //возвращаем код города
        }else {
            return jsonDataCity; //возвращаем JSON городов
        }

    }



    private String getCode(String jsonDataCity, String cityRu){
        String code = null;
        Object objectJsonCodeCityDocument = null;

        try {
            //получаем нужный нам код из множества
            objectJsonCodeCityDocument = new JSONParser().parse(jsonDataCity);
            JSONArray getObjectJsonCodeCity = (JSONArray) objectJsonCodeCityDocument;
            Iterator codeCityIter = getObjectJsonCodeCity.iterator();
            int count = 0;
            while (codeCityIter.hasNext()) {
                JSONObject cityCodeObj = (JSONObject) codeCityIter.next();
                if (cityCodeObj.get("ss") != null) {
                    try {
                        if ((cityCodeObj.get("n").toString().substring(0, cityRu.length()).equals(cityRu))) {
                            code = String.valueOf(cityCodeObj.get("c"));
                            count++;
                        }
                    } catch (StringIndexOutOfBoundsException ex) {
                        //ignore
                    }
                }
            }

            if (count == 0) {
                objectJsonCodeCityDocument = new JSONParser().parse(jsonDataCity);
                JSONArray newJsonArray = (JSONArray) objectJsonCodeCityDocument;
                Iterator newCodeCity = newJsonArray.iterator();
                while (newCodeCity.hasNext()) {
                    JSONObject cityCodeObj = (JSONObject) newCodeCity.next();
                    try {
                        if ((cityCodeObj.get("n").toString().substring(0, cityRu.length()).equals(cityRu))) {
                            code = String.valueOf(cityCodeObj.get("c"));
                            break;
                        }
                    } catch (StringIndexOutOfBoundsException ex) {
                        //ignore
                    }

                }
            }

        } catch (ParseException e) {
            //ignore
        }


        return code;
    }



    void writeDB(String from, String to, String date, String maxPrice, String idUser){

        java.sql.Connection connection = new ConnectionDB().connect(); //из-за того что два Connection (Jsoup и SQL)
        String insert = "INSERT INTO needTickets(\"from\", \"to\", date, maxPrice, idUser) VALUES(?,?,?,?,?)";

        PreparedStatement prSt = null;
        try {
            prSt = connection.prepareStatement(insert);
            prSt.setString(1, from.toUpperCase());
            prSt.setString(2, to.toUpperCase());
            prSt.setString(3, date);
            prSt.setString(4, maxPrice);
            prSt.setString(5, idUser);
            prSt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



}





