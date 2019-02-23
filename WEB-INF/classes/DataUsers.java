import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;

class DataUsers {
    //for searching
    private String[] param;
    private boolean searchNewTickets;
    private boolean notTickets;
    private ArrayList<String> dataTickets;

    //for editing
    private  java.sql.Connection connectionDB;
    private ResultSet resultSet;
    private StringBuilder dataQueries;
    private int countQueries = 1;
    private int numberTicketForEdit = 0;
    private int stagesForEditing = -1; //variable for stages
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



    String[] getParam() {
        return param;
    }

    boolean getSearchNewTickets() {
        return searchNewTickets;
    }

    boolean getNotTickets() {
        return notTickets;
    }

    ArrayList<String> getDataTickets() {
        return dataTickets;
    }

    Connection getConnectionDB() {
        return connectionDB;
    }

    ResultSet getResultSet() {
        return resultSet;
    }

    StringBuilder getDataQueries() {
        return dataQueries;
    }

    int getCountQueries() {
        return countQueries;
    }

    int getNumberTicketForEdit() {
        return numberTicketForEdit;
    }

    int getStagesForEditing() {
        return stagesForEditing;
    }


    void setParam(String[] param) {
        this.param = param;
    }

    void setSearchNewTickets(boolean searchNewTickets) {
        this.searchNewTickets = searchNewTickets;
    }

    void setNotTickets(boolean notTickets) {
        this.notTickets = notTickets;
    }

    void setDataTickets(ArrayList<String> dataTickets) {
        this.dataTickets = dataTickets;
    }

    void setConnectionDB(Connection connectionDB) {
        this.connectionDB = connectionDB;
    }

    void setResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    void setDataQueries(StringBuilder dataQueries) {
        this.dataQueries = dataQueries;
    }

    void setCountQueries(int countQueries) {
        this.countQueries = countQueries;
    }

    void setNumberTicketForEdit(int numberTicketForEdit) {
        this.numberTicketForEdit = numberTicketForEdit;
    }

    void setStagesForEditing(int stagesForEditing) {
        this.stagesForEditing = stagesForEditing;
    }
}
