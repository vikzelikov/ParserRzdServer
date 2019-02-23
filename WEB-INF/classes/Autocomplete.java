import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Iterator;

@WebServlet("/autocomplete")
public class Autocomplete extends HttpServlet {

    private Main main = new Main();
    private Bot bot = new Bot();
    private String city = null;
    private String masCities = null;
    private String[] arr = null;
    private String[] cityType = null;
    private String type = null;
    private Object objectJsonCodeCityDocument = null;




    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        System.out.println("ЗАПРОС");

        city = bot.getBodyDecoder(req);
        city = URLDecoder.decode(city, "UTF-8");
        cityType = city.split("//");
        type = cityType[1];

        city = cityType[0].toUpperCase().trim();
        masCities = main.getCity(city, false); //получаем кучу городов
        int id = 0;




        if(!masCities.equals("")) {
            try {
                objectJsonCodeCityDocument = new JSONParser().parse(masCities);
                JSONArray getObjectJsonCodeCity = (JSONArray) objectJsonCodeCityDocument;
                Iterator codeCityIter = getObjectJsonCodeCity.iterator();
                while (codeCityIter.hasNext()) {
                    JSONObject cityCodeObj = (JSONObject) codeCityIter.next();
                    try {
                        arr = cityCodeObj.get("n").toString().split(" ");
                        if (arr.length > 1) {
                            for (String str : arr) {
                                int end;
                                if (city.length() > str.length()) {
                                    end = str.length();
                                } else {
                                    end = city.length();
                                }

                                if (city.equals(str.substring(0, end))) {
                                    resp.getWriter().print("<p class='cities' id=t"+id+" style='border:2px inline #383838 !important;margin:3px;padding:3px;background:#f4f4f4;cursor:pointer;'>" + cityCodeObj.get("n").toString() + "</p>");
                                    id++;
                                }
                            }
                        } else {
                            if ((arr[0].substring(0, city.length()).equals(city))) {
                                resp.getWriter().print("<p class='cities' id=t"+id+" style='border:2px inline #383838 !important;margin:3px;padding:3px;background:#f4f4f4;cursor:pointer;'>" + cityCodeObj.get("n").toString() + "</p>");
                                id++;
                            }
                        }
                    } catch (StringIndexOutOfBoundsException ex) {
                        //ignore
                    }

                }
                resp.getWriter().print("<script>$('.cities').click(function(){$('#"+type+"').val($(this).text().toLowerCase());$('#result"+type+"').html('');});</script>");
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

    }
}
