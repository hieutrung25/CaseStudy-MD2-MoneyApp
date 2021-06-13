package controller;

import iservice.MoneyTypeManagement;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import model.MoneyType;
import model.Money;

import java.io.*;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;

public class TransactionController implements Initializable {
    //  all transaction components
    @FXML
    private TableView<Money> allTransactionTable;
    @FXML
    private TableColumn<Money, LocalDate> transactionDateColumn;
    @FXML
    private TableColumn<Money, Long> transactionAmountColumn;
    @FXML
    private TableColumn<Money, MoneyType> transactionMoneyTypeColumn;
    @FXML
    private TableColumn<Money, String> transactionDetailColumn;
    @FXML
    private Label totalIncomeLabel;
    @FXML
    private Label totalOutcomeLabel;
    @FXML
    private Label realMoneyLabel;
    @FXML
    private Button resetBtn;
    @FXML
    private ChoiceBox sortChoiceBox;

    // edit transaction components
    @FXML
    private Label amountNoticeLabel;
    @FXML
    private Button saveTransactionBtn;
    @FXML
    private TextField amountText;
    @FXML
    private RadioButton incomeRadioBtn;
    @FXML
    private RadioButton outcomeRadioBtn;
    @FXML
    private ChoiceBox transactionGroup;
    @FXML
    private TextField transactionDescription;
    @FXML
    private DatePicker transactionDate;
    @FXML
    private TextField hiddenUUID;

    private static ObservableList<Money> transactionList;
    private long totalIncome = 0;
    private long totalOutcome = 0;
    private Money selectedTableTransaction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        addCommaAmountTextfield();
        transactionDateColumn.setSortType(TableColumn.SortType.DESCENDING);
        transactionList = FXCollections.observableArrayList(readFile());
        allTransactionTable.setPlaceholder(new Label("Bạn chưa có giao dịch nào."));
        addTransactionToTable(transactionList);
        allTransactionTable.refresh();
        amountText.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                amountText.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        // disable future date in datepicker
//        vô hiệu hóa ngày trong tương lai trong trình chọn ngày
        transactionDate.setDayCellFactory(param -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.compareTo(LocalDate.now()) > 0);
            }
        });
        getOverviewNum();
        setLabel();
        setOptionSortChoiceBox();
        sortChoiceBox.setValue("Tất cả giao dịch");
        checkRequiredFields();
        setColorForTableCell();

    }


    // Validate amount text field
    public void validateAmountTextField() {
        amountText.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("^\\d+$") && !newValue.isEmpty()) {
                saveTransactionBtn.setDisable(true);
                amountNoticeLabel.setVisible(true);
            } else {
                saveTransactionBtn.setDisable(false);
                amountNoticeLabel.setVisible(false);
            }
        });
    }

    // When choose income or outcome, the dropdown list of transaction types are changed
//    Khi chọn thu nhập hoặc kết quả, danh sách thả xuống của các loại giao dịch được thay đổi
    public void chooseIncomeTransaction(ActionEvent actionEvent) {
        System.out.println("User choose income transaction");
        transactionGroup.getItems().clear();
        List<MoneyType> incomeTypes = new MoneyTypeManagement().findIncomeTypeList();
        for (int i = 0; i < incomeTypes.size(); i++) {
            transactionGroup.getItems().add(incomeTypes.get(i));
        }

    }

    public void chooseOutcomeTransaction(ActionEvent actionEvent) {
        System.out.println("User choose outcome transaction");
        transactionGroup.getItems().clear();
        List<MoneyType> outcomeTypes = new MoneyTypeManagement().findOutcomeTypeList();
        for (int i = 0; i < outcomeTypes.size(); i++) {
            transactionGroup.getItems().add(outcomeTypes.get(i));
        }
    }


    // Save button
    public void saveTransaction(ActionEvent actionEvent) {
        if (hiddenUUID.getText().trim().isEmpty()) {
            Money money = getInputMoneyObj();
            generateUUID(money);
            transactionList.add(money);
        } else {
            updateTransaction();
        }
        saveTransactionToTable();
        sortChoiceBox.setValue("Tất cả giao dịch");

    }

    public void generateUUID(Money money) {
        UUID uuid = UUID.randomUUID();
        money.setUuid(uuid);
    }

    public void updateTransaction() {
        Money money = selectedTableTransaction;
        editTransaction(money);
        saveTransactionToTable();
    }

    public void saveTransactionToTable() {
        sortTransactionList();
        addTransactionToTable(transactionList);
        allTransactionTable.refresh();
        clearInputFields();
        System.out.println(transactionList);
        getOverviewNum();
        setLabel();
    }

    public Money getInputMoneyObj() {
        Money inputMoneyObj;
        long inputAmount = Long.parseLong(amountText.getText());
        boolean isIncome = true;
        if (incomeRadioBtn.isSelected() && transactionGroup.getValue() != null) {
            isIncome = true;
        } else if (outcomeRadioBtn.isSelected() && transactionGroup.getValue() != null) {
            isIncome = false;
        }
        MoneyType moneyType = (MoneyType) transactionGroup.getValue();
        String inputDescription = transactionDescription.getText();
        LocalDate inputDate = transactionDate.getValue();
        inputMoneyObj = new Money(inputAmount, isIncome, inputDescription, moneyType, inputDate);
        return inputMoneyObj;
    }

    public void clearInputFields() {
        amountText.clear();
        transactionDescription.clear();
        transactionDate.getEditor().clear();
        transactionGroup.getItems().clear();
        outcomeRadioBtn.setSelected(false);
        incomeRadioBtn.setSelected(false);
        hiddenUUID.clear();
    }

    // Add transactions to table: thêm giao dịch vào bảng
    public void addTransactionToTable(ObservableList<Money> list) {
        transactionDateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        transactionDetailColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        transactionMoneyTypeColumn.setCellValueFactory(new PropertyValueFactory<>("moneyType"));
        transactionAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("en", "US"));
        transactionAmountColumn.setCellFactory(tc -> new TableCell<Money, Long>() {

            @Override
            protected void updateItem(Long amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(amount));
                }
            }
        });
        allTransactionTable.setItems(list);
    }

    private void getOverviewNum() {
        long income = 0;
        long outcome = 0;
        for (int i = 0; i < transactionList.size(); i++) {
            if (transactionList.get(i).isIncome())
                income += transactionList.get(i).getAmount();
            else
                outcome += transactionList.get(i).getAmount();
        }
        totalIncome = income;
        totalOutcome = outcome;
    }

    private void setLabel() {
        String incomeLabel = addCommaToMoney(String.valueOf(totalIncome));
        totalIncomeLabel.setText(incomeLabel);
        String outcomeLabel = addCommaToMoney(String.valueOf(totalOutcome));
        totalOutcomeLabel.setText(outcomeLabel);
        long realMoney = totalIncome - totalOutcome;
        String realMoneyLb = addCommaToMoney(String.valueOf(realMoney));
        realMoneyLabel.setText(realMoneyLb);
    }

    // Hành động khi người dùng nhấn vào một ô trong bảng
    public void confirmUserAction(Money selectedMoney) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận thao tác");
        alert.setHeaderText(null);
        alert.setContentText("Lựa chọn thao tác bạn muốn thực hiện");

        ButtonType buttonEdit = new ButtonType("Chỉnh sửa");
        ButtonType buttonDelete = new ButtonType("Xóa");
        ButtonType buttonCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonEdit, buttonDelete, buttonCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonEdit) {
            editActionSelected(selectedMoney);
        } else if (result.get() == buttonDelete) {
            confirmDeleteDialog(selectedMoney);
        } else {
            alert.close();
        }
    }

    public void getSelectedItem(MouseEvent click) {
        if (click.getClickCount() == 2) {
            selectedTableTransaction = allTransactionTable.getSelectionModel().getSelectedItem();
            confirmUserAction(selectedTableTransaction);
        }
    }

    public void confirmDeleteDialog(Money money) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận thao tác");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn xóa giao dịch này?");

        ButtonType buttonYes = new ButtonType("Xóa");
        ButtonType buttonCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonYes, buttonCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonYes) {
            deleteActionSelected(money);
        } else {
            alert.close();
        }
    }


    public void editActionSelected(Money Obj) {
        displayValueForEdit(Obj);
        System.out.println("old" + Obj);
    }

    public void editTransaction(Money Obj) {
        Obj.setAmount(Long.parseLong(amountText.getText()));
        Obj.setMoneyType((MoneyType) transactionGroup.getValue());
        Obj.setDescription(transactionDescription.getText());
        Obj.setDate(transactionDate.getValue());
        boolean isIncome = true;
        if (incomeRadioBtn.isSelected() && transactionGroup.getValue() != null) {
            isIncome = true;
        } else if (outcomeRadioBtn.isSelected() && transactionGroup.getValue() != null) {
            isIncome = false;
        }
        Obj.setIncome(isIncome);
        System.out.println("new obj" + Obj);
    }

    public void deleteActionSelected(Money Obj) {
        System.out.println("Delete");
        transactionList.remove(Obj);
        addTransactionToTable(transactionList);
        getOverviewNum();
        setLabel();
    }

    public void displayValueForEdit(Money Obj) {
        hiddenUUID.setText(Obj.getUuid());
        System.out.println(Obj.getMoneyType().toString());
        transactionGroup.setValue(Obj.getMoneyType().getName());
        amountText.setText(String.valueOf(Obj.getAmount()));
        if (Obj.isIncome())
            incomeRadioBtn.setSelected(true);
        else
            outcomeRadioBtn.setSelected(true);
        transactionDescription.setText(Obj.getDescription());
        transactionDate.setValue(Obj.getDate());
    }

    //readFile and writeFile
    public void writeFile() {
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("transaction.dat"));
            for (Money money : transactionList) {
                objectOutputStream.writeObject(money);
            }
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("bye bye");

    }

    public List<Money> readFile() {
        List<Money> list = new ArrayList<>();
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("transaction.dat"));

            while (true) {
                list.add((Money) objectInputStream.readObject());
            }
        } catch (EOFException e) {
            e.getMessage();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return list;
    }

    //reset all
    public void confirmReset() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận thao tác");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn thực hiện thao tác này?\n Mọi giao dịch của bạn sẽ bị xóa và không thể khôi phục.");

        ButtonType buttonYes = new ButtonType("Đồng ý");
        ButtonType buttonCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonYes, buttonCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonYes) {
            transactionList.clear();
            saveTransactionToTable();
        } else {
            alert.close();
        }
    }

    // code dùng để tạo hộp lựa chọn tính năng
    public void setOptionSortChoiceBox() {
        ObservableList<String> options =
                FXCollections.observableArrayList(
                        "Tất cả giao dịch",
                        "Thu nhập",
                        "Chi tiêu"
                );
        sortChoiceBox.setItems(options);
        sortChoiceBox.getSelectionModel().selectedIndexProperty().addListener((v, oldValue, newValue) -> sortChoiceboxSelected((Integer) newValue));
    }

    public ObservableList<Money> getOnlyIncomeList() {
        ObservableList<Money> onlyIncomeList = FXCollections.observableArrayList();
        for (int i = 0; i < transactionList.size(); i++) {
            if (transactionList.get(i).isIncome())
                onlyIncomeList.add(transactionList.get(i));
        }
        return onlyIncomeList;
    }

    public ObservableList<Money> getOnlyOutcomeList() {
        ObservableList<Money> onlyOutcomeList = FXCollections.observableArrayList();
        for (int i = 0; i < transactionList.size(); i++) {
            if (!transactionList.get(i).isIncome())
                onlyOutcomeList.add(transactionList.get(i));
        }
        return onlyOutcomeList;
    }

    public void sortChoiceboxSelected(int number) {
        if (number == 0) {
            sortTransactionList();
            addTransactionToTable(transactionList);
        } else if (number == 1)
            addTransactionToTable(getOnlyIncomeList());
        else
            addTransactionToTable(getOnlyOutcomeList());
        allTransactionTable.refresh();
    }

    // kiểm tra code và chức năng bấm
    public void checkRequiredFields() {
        saveTransactionBtn.disableProperty().bind(
                amountText.textProperty().isEmpty()
                        .or(transactionGroup.valueProperty().isNull())
                        .or(transactionDate.valueProperty().isNull()));
    }

    public void sortTransactionList() {
        Collections.sort(transactionList, new Comparator<Money>() {
            @Override
            public int compare(Money o1, Money o2) {
                return (o1.getDate().isBefore(o2.getDate()) ? 1 : -1);
            }
        });
    }

    public String addCommaToMoney(String string) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("en", "US"));
        String price = nf.format(Integer.parseInt(string));
        return price;
    }

    public void addCommaAmountTextfield() {
        amountText.focusedProperty().addListener((observable, oldValue, newValue) -> {
            NumberFormat formatter = NumberFormat.getInstance(new Locale("en", "US"));
            String newValueStr = formatter.format(newValue);
            amountText.setText(newValueStr);
        });
    }

    public void setColorForTableCell() {
        allTransactionTable.setRowFactory(tv -> new TableRow<Money>() {
            @Override
            protected void updateItem(Money money, boolean empty) {
                super.updateItem(money, empty);
                if (money == null)
                    setStyle("");
                else{
                    if (money.isIncome()){
                        transactionAmountColumn.setStyle("-fx-text-fill: blue");
                        transactionDateColumn.setStyle("-fx-text-fill: blue");
                        transactionMoneyTypeColumn.setStyle("-fx-text-fill: blue");
                        transactionDetailColumn.setStyle("-fx-text-fill: blue");
                    }

                    else{
                        transactionAmountColumn.setStyle("-fx-text-fill: red");
                        transactionDateColumn.setStyle("-fx-text-fill: red");
                        transactionMoneyTypeColumn.setStyle("-fx-text-fill: red");
                        transactionDetailColumn.setStyle("-fx-text-fill: red");
                    }
                }
            }
        });

    }

}


