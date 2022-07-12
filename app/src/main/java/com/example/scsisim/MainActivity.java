package com.example.scsisim;

import com.jjoe64.graphview.*;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    GraphView graph = null;
    int pauseInterval = 2;  //время до следующего действия после выполнения предыдущего
    int busBitrate = 8; // разрядность шины
    int deltaTime = 1; //число тактов, на которые устанавливаются сигналы
    int ReqAckOffset = 1; // допустимое ЧНП/ЧНЗ
    int dataLengthMin = 10;
    int dataLengthMax = 50;
    int randomPauseMultiplier = 5;
    EditText MLText = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        graph = (GraphView) findViewById(R.id.view);
        MLText = (EditText) findViewById(R.id.editTextTextMultiLine);
        graph.setTitle("SCSI Временная диаграмма");
    }
    private String invertString(String str)
    {
        String res = "";
        for(int i = 0; i < str.length(); i++)
        {
            if(str.charAt(i) == '0') res += '1';
            else if(str.charAt(i) == '1') res += '0';
            else res += str.charAt(i);
        }
        return res;
    }
    private String negString(String str)
    {
        String res = "";
        for(int i = 0; i < str.length(); i++)
        {
            if(str.charAt(i) == '0') res += '0';
            else if(str.charAt(i) == '1') res += "-1";
            else res += str.charAt(i);
        }
        return res;
    }
    private ArrayList<Integer> stringToSequence(String str)
    {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        for(int i = 0; i < str.length(); i++)
        {
            if(str.charAt(i) == '0'){ arr.add(0); }
            else if (str.charAt(i) == '-'){ arr.add(-1); i++; }
            else arr.add(1);
        }
        return arr;
    }
    private LineGraphSeries<DataPoint> sequenceToSeries(ArrayList<Integer> data)
    {
        //func(seq -> series)
        // input: ArrayList<Integer> data
        // output: LineGraphSeries<DataPoint> series1
        ArrayList<DataPoint> graphPoints = new ArrayList<DataPoint>();
        double yVar = 0.0;
        double xVar = 0.0;
        for(int i = 0; i < data.size(); i++)
        {
            yVar = data.get(i);
            graphPoints.add(new DataPoint(xVar,yVar));
            xVar = xVar + 1;
        }
        //ArrayList -> DataPoint[]
        DataPoint[] xx = new DataPoint[graphPoints.size()];
        for(int i = 0; i < graphPoints.size(); i++)
        {
            xx[i] = graphPoints.get(i);
        }
        LineGraphSeries<DataPoint> series1 = new LineGraphSeries<DataPoint>(xx);
        series1.setColor(Color.BLACK);
        return series1;
    }
    private ArrayList<Integer> moveSeqUpDown(ArrayList<Integer> src, int delta)
    {
        for(int i = 0; i < src.size(); i++)
            src.set(i, src.get(i)+delta);
        return src;
    }
    public void asyncRead(View view) {
        MLText.getText().clear();
        graph.removeAllSeries();
        EditText etData = (EditText)findViewById(R.id.editTextTextPersonName);
        int cntr = (int) (Math.random()*(dataLengthMax-dataLengthMin)+dataLengthMin);
        String readData = "";
        for(int i = 0; i < cntr; i++)
        {
            if(Math.random()<0.5)
                readData += '0';
            else
                readData += '1';
        }
        etData.setText(readData);
        //указатель на текущий символ передаваемых данных и их общее число
        int dataPtr = 0;
        int dataLength = readData.length();
        String InputOutputType = "0";
        String DataBus = "0";
        String Req = "0";
        String Ack = "0";
        //1. start
        InputOutputType = setSignal(InputOutputType);
        DataBus = waitSignal(DataBus);
        Req = waitSignal(Req);
        Ack = waitSignal(Ack);
        while(dataPtr < dataLength)
        {
            //2. set DB
            DataBus = setSignal(DataBus);
            Req = waitSignal(Req);
            Ack = waitSignal(Ack);
            //3. set Req
            Req = setSignal(Req);
            DataBus = waitSignal(DataBus);
            Ack = waitSignal(Ack);
            //4. set Ack
            Ack = setSignal(Ack);
            DataBus = waitSignal(DataBus);
            Req = waitSignal(Req);
            //5. drop DB, Req
            DataBus = dropSignal(DataBus);
            Req = dropSignal(Req);
            Ack = waitSignal(Ack);
            //6. drop Ack
            Ack = dropSignal(Ack);
            DataBus = waitSignal(DataBus);
            Req = waitSignal(Req);
            InputOutputType = waitSignal(InputOutputType);
            InputOutputType = waitSignal(InputOutputType);
            InputOutputType = waitSignal(InputOutputType);
            InputOutputType = waitSignal(InputOutputType);
            InputOutputType = waitSignal(InputOutputType);
            String substr = "";
            for(int i = 0; i < busBitrate; i++)
            {
                if(i+dataPtr >= readData.length())
                    break;
                substr += readData.charAt(i+dataPtr);
            }
            MLText.append("\nПередано: "+substr);
            dataPtr += busBitrate;

        }
        //end.
        InputOutputType = dropSignal(InputOutputType);
        Req = waitSignal(Req);
        Ack = waitSignal(Ack);
        DataBus = waitSignal(DataBus);

        String invertedReq = invertString(Req);
        String invertedAck = invertString(Ack);
        String invertedInputOutputType = invertString(InputOutputType);
        //костыль: инвертируем и соединяем 2 линии DB для <_>
        String invertedDataBus = negString(DataBus);

        //добавляем линии на график
        lineSeriesToGraph(sequenceToSeries(stringToSequence(DataBus)),"DB#",Color.BLACK, 3);
        lineSeriesToGraph(sequenceToSeries(stringToSequence(invertedDataBus)),"",Color.BLACK, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertedReq),3)),"REQ#",Color.BLUE, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertedAck),6)),"ACK#",Color.RED, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertedInputOutputType),9)),"I/O#",Color.CYAN, 3);

        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollable(true);
        // set manual X bounds
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        // set manual Y bounds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-1);
        graph.getViewport().setMaxY(11);
        graph.getLegendRenderer().setVisible(true);
    }
    public void asyncWrite(View view) {
        MLText.getText().clear();
        graph.removeAllSeries();
        EditText etData = (EditText)findViewById(R.id.editTextTextPersonName);
        String inputData = etData.getText().toString();
        //указатель на текущий символ передаваемых данных и их общее число
        int dataPtr = 0;
        int dataLength = inputData.length();
        String InputOutputType = "1";
        String DataBus = "0";
        String Req = "0";
        String Ack = "0";
        //1. start
        InputOutputType = dropSignal(InputOutputType);
        DataBus = waitSignal(DataBus);
        Req = waitSignal(Req);
        Ack = waitSignal(Ack);
        while(dataPtr < dataLength)
        {
            //set req
            Req = setSignal(Req);
            DataBus = waitSignal(DataBus);
            Ack = waitSignal(Ack);
            //set db
            DataBus = setSignal(DataBus);
            Req = waitSignal(Req);
            Ack = waitSignal(Ack);
            //set ack
            Ack = setSignal(Ack);
            DataBus = waitSignal(DataBus);
            Req = waitSignal(Req);
            //drop req
            Req = dropSignal(Req);
            DataBus = waitSignal(DataBus);
            Ack = waitSignal(Ack);
            //drop db,ack
            DataBus = dropSignal(DataBus);
            Ack = dropSignal(Ack);
            Req = waitSignal(Req);
            InputOutputType = waitSignal(InputOutputType);
            InputOutputType = waitSignal(InputOutputType);
            InputOutputType = waitSignal(InputOutputType);
            InputOutputType = waitSignal(InputOutputType);
            InputOutputType = waitSignal(InputOutputType);
            String substr = "";
            for(int i = 0; i < busBitrate; i++)
            {
                if(i+dataPtr >= inputData.length())
                    break;
                substr += inputData.charAt(i+dataPtr);
            }
            MLText.append("\nПередано: "+substr);
            dataPtr += busBitrate;
        }
        //end.
        InputOutputType = setSignal(InputOutputType);
        Req = waitSignal(Req);
        Ack = waitSignal(Ack);
        DataBus = waitSignal(DataBus);

        String invertedReq = invertString(Req);
        String invertedAck = invertString(Ack);
        String invertedInputOutputType = invertString(InputOutputType);
        //костыль: инвертируем и соединяем 2 линии DB для <_>
        String invertedDataBus = negString(DataBus);

        //добавляем линии на график
        lineSeriesToGraph(sequenceToSeries(stringToSequence(DataBus)),"DB#",Color.BLACK, 3);
        lineSeriesToGraph(sequenceToSeries(stringToSequence(invertedDataBus)),"",Color.BLACK, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertedReq),3)),"REQ#",Color.BLUE, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertedAck),6)),"ACK#",Color.RED, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertedInputOutputType),9)),"I/O#",Color.CYAN, 3);

        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-1);
        graph.getViewport().setMaxY(11);
        graph.getLegendRenderer().setVisible(true);
    }
    public void syncRead(View view) {
        MLText.getText().clear();
        graph.removeAllSeries();
        EditText etData = (EditText)findViewById(R.id.editTextTextPersonName);
        int cntr = (int) (Math.random()*(dataLengthMax-dataLengthMin)+dataLengthMin);
        String readData = "";
        for(int i = 0; i < cntr; i++)
        {
            if(Math.random()<0.5)
                readData += '0';
            else
                readData += '1';
        }
        etData.setText(readData);
        //указатель на текущий символ передаваемых данных и их общее число
        int dataPtrSent = 0;
        int dataPtrGet = 0;
        int dataLength = readData.length();
        String InputOutputType = "1";
        String DataBus = "0";
        String Req = "0";
        String Ack = "0";
        //1. start
        InputOutputType = dropSignal(InputOutputType);
        DataBus = waitSignal(DataBus);
        Req = waitSignal(Req);
        Ack = waitSignal(Ack);
        int targetState = 0;    // 0 - старт, 1 - выставлены данные, 2 - выставлен Req, 10 - все передано
        int initiatorState = 0; // 0 - старт, 1 - выставлено Ack, 10 - всё подтверждено
        int unconfimedCntr = 0; // ЧНП/ЧНЗ
        int randomPauseCntr = 0;
        int AckState = 0;
        int DataBusState = 0;

        //тактирование
        while(true)
        {
            //ход init
            if(initiatorState == 0)
            {
                if(unconfimedCntr>0)
                {
                    if(AckState == 0)
                    {
                        for(int i = 0; i < deltaTime; i++) InputOutputType = waitSignal(InputOutputType);
                        for(int i = 0; i < deltaTime; i++) Ack = waitSignal(Ack);
                        AckState++;
                    }
                    else if(AckState == 1)
                    {
                        for(int i = 0; i < deltaTime; i++) InputOutputType = waitSignal(InputOutputType);
                        for(int i = 0; i < deltaTime; i++) Ack = setSignal(Ack);
                        AckState++;
                    }
                    else if(AckState == 2)
                    {
                        for(int i = 0; i < deltaTime; i++) InputOutputType = waitSignal(InputOutputType);
                        for(int i = 0; i < deltaTime; i++) Ack = dropSignal(Ack);
                        unconfimedCntr--;
                        String substr = "";
                        for(int i = 0; i < busBitrate; i++)
                        {
                            if(i+dataPtrGet >= readData.length())
                                break;
                            substr += readData.charAt(i+dataPtrGet);
                        }
                        MLText.append("\nПередано: "+substr);
                        dataPtrGet+=busBitrate;
                        initiatorState = 1;
                        AckState = 0;
                        double randomNum = Math.random();
                        randomNum = randomNum*randomPauseMultiplier*deltaTime;
                        randomPauseCntr = (int) randomNum;
                    }
                }
                else
                {
                    for(int i = 0; i < deltaTime; i++) InputOutputType = waitSignal(InputOutputType);
                    for(int i = 0; i < deltaTime; i++) Ack = waitSignal(Ack);
                }

            }
            else if (initiatorState == 1)
            {
                if(dataPtrGet >= dataLength)   //все данные переданы
                    initiatorState = 10;
                else
                {
                    for(int i = 0; i < deltaTime; i++) InputOutputType = waitSignal(InputOutputType);
                    for(int i = 0; i < deltaTime; i++) Ack = waitSignal(Ack);
                    randomPauseCntr = randomPauseCntr - deltaTime;
                    if(randomPauseCntr <= 0)
                        initiatorState = 0;
                }
            }
            //ход target
            if(targetState == 2)
            {
                if(dataPtrSent >= dataLength)   //все данные переданы
                    targetState = 10;
                else if(unconfimedCntr>=ReqAckOffset)
                {
                    for(int i = 0; i < deltaTime; i++) Req = waitSignal(Req);
                    for(int i = 0; i < deltaTime; i++) DataBus = waitSignal(DataBus);
                }
                else
                {
                    targetState = 0;
                }
            }
            if(targetState == 0)
            {
                if(DataBusState == 0)
                {
                    for(int i = 0; i < deltaTime; i++) DataBus = setSignal(DataBus);
                    for(int i = 0; i < deltaTime; i++) Req = waitSignal(Req);
                    DataBusState++;
                    unconfimedCntr++;
                }
                else if(DataBusState == 1)
                {
                    for(int i = 0; i < deltaTime; i++) DataBus = waitSignal(DataBus);
                    for(int i = 0; i < deltaTime; i++) Req = setSignal(Req);
                    DataBusState++;
                }
                else if(DataBusState == 2)
                {
                    for(int i = 0; i < deltaTime; i++) DataBus = dropSignal(DataBus);
                    for(int i = 0; i < deltaTime; i++) Req = dropSignal(Req);
                    DataBusState = 0;
                    dataPtrSent+=busBitrate;
                    targetState = 2;
                }
            }
            // все данные переданы
            if((targetState == 10) && (initiatorState == 10))
                break;
        }
        for(int i = 0; i < 2*deltaTime; i++) InputOutputType = setSignal(InputOutputType);
        for(int i = 0; i < 2*deltaTime; i++) Req = waitSignal(Req);
        for(int i = 0; i < 2*deltaTime; i++) Ack = waitSignal(Ack);
        for(int i = 0; i < 2*deltaTime; i++) DataBus = waitSignal(DataBus);

        //выравнивание строк
        int strDiff = Ack.length() - Req.length();
        if(strDiff > 0)
            for(int i = 0; i < strDiff; i+=pauseInterval)
            {
                Req = waitSignal(Req);
                DataBus = waitSignal(DataBus);
            }
        else if(strDiff < 0)
            for(int i = 0; i < -strDiff; i+=pauseInterval)
            {
                Ack = waitSignal(Ack);
                InputOutputType = waitSignal(InputOutputType);
            }


        String invertedReq = invertString(Req);
        String invertedAck = invertString(Ack);
        String invertedInputOutputType = invertString(InputOutputType);
        //костыль: инвертируем и соединяем 2 линии DB для <_>
        String invertedDataBus = negString(DataBus);

        //добавляем линии на график
        lineSeriesToGraph(sequenceToSeries(stringToSequence(DataBus)),"DB#",Color.BLACK, 3);
        lineSeriesToGraph(sequenceToSeries(stringToSequence(invertedDataBus)),"",Color.BLACK, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertedReq),3)),"REQ#",Color.BLUE, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertedAck),6)),"ACK#",Color.RED, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertedInputOutputType),9)),"I/O#",Color.CYAN, 3);

        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-1);
        graph.getViewport().setMaxY(11);
        graph.getLegendRenderer().setVisible(true);
    }
    public void syncWrite(View view) {
        MLText.getText().clear();
        graph.removeAllSeries();
        EditText etData = (EditText)findViewById(R.id.editTextTextPersonName);
        String inputData = etData.getText().toString();
        //указатель на текущий символ передаваемых данных и их общее число
        int dataPtrSent = 0;
        int dataPtrGet = 0;
        int dataLength = inputData.length();
        String InputOutputType = "1";
        String DataBus = "0";
        String Req = "0";
        String Ack = "0";
        //1. start
        InputOutputType = dropSignal(InputOutputType);
        DataBus = waitSignal(DataBus);
        Req = waitSignal(Req);
        Ack = waitSignal(Ack);
        int targetState = 0;    // 0 - старт, 1 - выставлены данные, 2 - выставлен Req, 10 - все передано
        int initiatorState = 0; // 0 - старт, 1 - выставлено Ack, 10 - всё подтверждено
        int unconfimedCntr = 0; // ЧНП/ЧНЗ
        int randomPauseCntr = 0;
        int AckState = 0;
        int ReqState = 0;
        //тактирование
        while(true)
        {
            //ход target
            if(targetState == 1)
            {
                if(dataPtrGet >= dataLength)   //все данные переданы
                    targetState = 10;
                else if(unconfimedCntr>=ReqAckOffset)
                {
                    for(int i = 0; i < deltaTime; i++) Req = waitSignal(Req);
                }
                else
                {
                    targetState = 0;
                }
            }
            if(targetState == 0)
            {
                if(ReqState == 0)
                {
                    for(int i = 0; i < deltaTime; i++) Req = setSignal(Req);
                    ReqState++;
                    unconfimedCntr++;
                }
                else if(ReqState == 1)
                {
                    for(int i = 0; i < deltaTime; i++) Req = dropSignal(Req);
                    ReqState = 0;
                    String substr = "";
                    for(int i = 0; i < busBitrate; i++)
                    {
                        if(i+dataPtrGet >= inputData.length())
                            break;
                        substr += inputData.charAt(i+dataPtrGet);
                    }
                    MLText.append("\nПередано: "+substr);
                    dataPtrGet+=busBitrate;
                    targetState = 1;
                }
            }
            //ход init
            if(initiatorState == 0)
            {
                if(unconfimedCntr>0)
                {
                    if(AckState == 0)
                    {
                        for(int i = 0; i < deltaTime; i++) InputOutputType = waitSignal(InputOutputType);
                        for(int i = 0; i < deltaTime; i++) Ack = waitSignal(Ack);
                        for(int i = 0; i < deltaTime; i++) DataBus = setSignal(DataBus);
                        AckState++;

                    }
                    else if(AckState == 1)
                    {
                        for(int i = 0; i < deltaTime; i++) InputOutputType = waitSignal(InputOutputType);
                        for(int i = 0; i < deltaTime; i++) Ack = setSignal(Ack);
                        for(int i = 0; i < deltaTime; i++) DataBus = waitSignal(DataBus);
                        AckState++;
                    }
                    else if(AckState == 2)
                    {
                        for(int i = 0; i < deltaTime; i++) InputOutputType = waitSignal(InputOutputType);
                        for(int i = 0; i < deltaTime; i++) Ack = dropSignal(Ack);
                        for(int i = 0; i < deltaTime; i++) DataBus = dropSignal(DataBus);
                        unconfimedCntr--;
                        dataPtrSent+=busBitrate;
                        initiatorState = 1;
                        AckState = 0;
                        double randomNum = Math.random();
                        randomNum = randomNum*randomPauseMultiplier*deltaTime;
                        randomPauseCntr = (int) randomNum;
                    }
                }
                else
                {
                    for(int i = 0; i < deltaTime; i++) InputOutputType = waitSignal(InputOutputType);
                    for(int i = 0; i < deltaTime; i++) Ack = waitSignal(Ack);
                    for(int i = 0; i < deltaTime; i++) DataBus = waitSignal(DataBus);
                }

            }
            else if (initiatorState == 1)
            {
                if(dataPtrSent >= dataLength)   //все данные переданы
                    initiatorState = 10;
                else
                {
                    for(int i = 0; i < deltaTime; i++) InputOutputType = waitSignal(InputOutputType);
                    for(int i = 0; i < deltaTime; i++) Ack = waitSignal(Ack);
                    for(int i = 0; i < deltaTime; i++) DataBus = waitSignal(DataBus);
                    randomPauseCntr = randomPauseCntr - deltaTime;
                    if(randomPauseCntr <= 0)
                        initiatorState = 0;
                }
            }

            // все данные переданы
            if((targetState == 10) && (initiatorState == 10))
                break;
        }
        for(int i = 0; i < 2*deltaTime; i++) InputOutputType = setSignal(InputOutputType);
        for(int i = 0; i < 2*deltaTime; i++) Req = waitSignal(Req);
        for(int i = 0; i < 2*deltaTime; i++) Ack = waitSignal(Ack);
        for(int i = 0; i < 2*deltaTime; i++) DataBus = waitSignal(DataBus);

        //выравнивание строк
        int strDiff = Ack.length() - Req.length();
        if(strDiff > 0)
            for(int i = 0; i < strDiff; i+=pauseInterval)
            {
                Req = waitSignal(Req);
                DataBus = waitSignal(DataBus);
            }
        else if(strDiff < 0)
            for(int i = 0; i < -strDiff; i+=pauseInterval)
            {
                Ack = waitSignal(Ack);
                InputOutputType = waitSignal(InputOutputType);
            }

        String invertedReq = invertString(Req);
        String invertedAck = invertString(Ack);
        String invertedInputOutputType = invertString(InputOutputType);
        //костыль: инвертируем и соединяем 2 линии DB для <_>
        String invertedDataBus = negString(DataBus);

        //добавляем линии на график
        lineSeriesToGraph(sequenceToSeries(stringToSequence(DataBus)),"DB#",Color.BLACK, 3);
        lineSeriesToGraph(sequenceToSeries(stringToSequence(invertedDataBus)),"",Color.BLACK, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertedReq),3)),"REQ#",Color.BLUE, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertedAck),6)),"ACK#",Color.RED, 3);
        lineSeriesToGraph(sequenceToSeries(moveSeqUpDown(stringToSequence(invertedInputOutputType),9)),"I/O#",Color.CYAN, 3);

        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-1);
        graph.getViewport().setMaxY(11);
        graph.getLegendRenderer().setVisible(true);
    }

    private String setDB(String DataBus, String Data, int DataPtr, int DataLength)
    {
        if(DataLength - DataPtr < busBitrate)
        {
            //данные нужно дополнить до полного пакета
            for(int i = 0; i < DataLength - DataPtr; i++)
                Data += "0";
        }
        //поставить 1 в DB
        //пауза n-1 тактов - добавить n-1 единиц
        for(int i = 0; i < pauseInterval; i++)
            DataBus += "1";
        return DataBus;
    }
    private void lineSeriesToGraph(LineGraphSeries<DataPoint> series1, String title, int color, int thickness)
    {
        series1.setTitle(title);
        series1.setColor(color);
        series1.setThickness(thickness);
        graph.addSeries(series1);
    }
    private String setSignal(String Signal)
    {
        //поставить 1
        //пауза n-1 тактов - добавить n-1 единиц
        for(int i = 0; i < pauseInterval; i++)
            Signal += "1";
        return Signal;
    }
    private String dropSignal(String Signal)
    {
        //поставить 1
        //пауза n-1 тактов - добавить n-1 единиц
        for(int i = 0; i < pauseInterval; i++)
            Signal += "0";
        return Signal;
    }
    private String waitSignal(String Signal)
    {
        //повтор пред сост n раз
        for(int i = 0; i < pauseInterval; i++)
            Signal += Signal.charAt(Signal.length()-1);
        return Signal;
    }

}