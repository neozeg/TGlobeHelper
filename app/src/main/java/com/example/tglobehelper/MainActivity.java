package com.example.tglobehelper;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tglobehelper.features.CallbackBundle;
import com.example.tglobehelper.features.OpenFileDialog;
import com.example.tglobehelper.features.SaveFileDialog;
import com.example.tglobehelper.features.UserTable;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private final static String TAG = "NotepadActivity";
    private final static int OPEN_FILE_DIALOG_ID = 101;
    private final static int SAVE_FILE_DIALOG_ID = 102;

    private final static int MAX_QUESTION_NUM = 83;

    private final static int MSG_PROGRESS_DIALOG = 10;
    private final static int MSG_PROGRESS_DIALOG_DISSMISS = 11;
    private final static String EXTRA_STRING = "extra_string";


    private final static String EXTRA_Q_NAME = "EXTRA_Q_NAME";
    private final static String EXTRA_Q_TYPE = "EXTRA_Q_TYPE";
    private final static String EXTRA_Q_TYPE2 = "EXTRA_Q_TYPE2";
    private final static String EXTRA_Q_RANGE = "EXTRA_Q_RANGE";
    private final static String EXTRA_Q_TOTALANS = "EXTRA_Q_TOTALANS";
    private final static String EXTRA_Q_HELP = "EXTRA_Q_HELP";

    private final static String FIRST_COMMENT = "//Q_Range,Q_Type,Q_Type2,Q_TotalAns,Q_Help,rght_cnt,wrong_cnt,reserved\n";
    private final static String FIRST_8Bytes = "DW\t0,0,0,0,0,0,0,0\n";
    private final static String SUBLINE1_COMMENT = ";Right answer";
    private final static String SUBLINE2_COMMENT = ";Help answer";
    private final static String SUBLINE3_COMMENT = ";???";
    //private String currentPath = "/sdcard/TGlobe/";
    private final static String OPEN_PATH = "/sdcard/TGlobe/";
    private final static String SAVE_PATH = "/sdcard/TGlobe/converted/";
    private String currentFileName = "Table.txt";
    private String currentFilePath = "";

    //private final static String FILTER_PATTERN = "\\w{10,}";
    private final static String FILTER_PATTERN = "SPI_SN\\d{4}";
    private final static String NUMBER_PATTERN = " \\d{1,}";
    private final static String USER_END_WORD = "SPI_SN_END";
    private final static String USER_DIVIDER = ",\t";

    private final static String[] FIRST_COMMENTS =
            {
                    "Q_Range",
                    "Q_Type",
                    "Q_Type2",
                    "Q_TotalAns",
                    "Q_Help",
                    "rght_cnt",
                    "wrong_cnt",
                    "reserved"
            };


    private String[] NotExistSoundList;

    private List<Map<String,Object>> QuestionList;

    private int[] firstlineData = {0,0,0,0,0,0,0,0};

    //view components
    private Button mBtnFile,mBtnNormal,mBtnQ5,mBtnQ6,mBtnQ22,mBtnQ27,mBtnOpen,mBtnBatchSave;
    //private EditText mEtContent;
    private TextView mTvSource,mTvTarget;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    /**/
        NotExistSoundList = new String[UserTable.ALL_SOUNDS.length-UserTable.EXIST_SOUNDS.length];
        boolean isExist;
        int indexAllSnd=0;
        for(int i =0;i<NotExistSoundList.length;i++){

            for(int j=indexAllSnd;j<UserTable.ALL_SOUNDS.length;j++){
                isExist = false;
                String allSndStr = UserTable.ALL_SOUNDS[j];
                for(String exSndStr:UserTable.EXIST_SOUNDS){
                    if(allSndStr.equals(exSndStr)){
                        isExist = true;
                        break;
                    }
                }
                if(!isExist) {
                    NotExistSoundList[i] = allSndStr;
                    indexAllSnd = j+1;
                    break;
                }
            }
        }

        QuestionList = new ArrayList<Map<String, Object>>();
        for(int i=0;i<MAX_QUESTION_NUM;i++){
            Map<String,Object> map = new HashMap<String, Object>();
            map.put(EXTRA_Q_NAME,UserTable.QUESION_NAME[i]);
            map.put(EXTRA_Q_RANGE,UserTable.Q_RANGE[i]);
            map.put(EXTRA_Q_TYPE,UserTable.Q_TYPE[i]);
            map.put(EXTRA_Q_TYPE2,UserTable.Q_TYPE2[i]);
            map.put(EXTRA_Q_TOTALANS,UserTable.Q_TOTALANS[i]);
            map.put(EXTRA_Q_HELP,UserTable.Q_HELP[i]);
            QuestionList.add(map);
        }

        setupViewComponents();


    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            /**/
            switch(msg.what){
                case MSG_PROGRESS_DIALOG:
                    mProgressDialog.setMax(msg.arg1);
                    mProgressDialog.setProgress(msg.arg2);
                    break;
                case MSG_PROGRESS_DIALOG_DISSMISS:
                    mTvSource.setText(msg.getData().getString(EXTRA_STRING));
                    mProgressDialog.dismiss();
                    break;
            }
        }
    };

    private void setupViewComponents(){
        mBtnFile = (Button) findViewById(R.id.buttonFile);
        mBtnNormal = (Button) findViewById(R.id.buttonNormal);
        mBtnQ5 = (Button) findViewById(R.id.buttonQ5);
        mBtnQ6 = (Button) findViewById(R.id.buttonQ6);
        mBtnQ22 = (Button) findViewById(R.id.buttonQ22);
        mBtnQ27 = (Button) findViewById(R.id.buttonQ27);
        mBtnOpen = (Button) findViewById(R.id.buttonOpen);
        mBtnBatchSave = (Button) findViewById(R.id.buttonBatchSave);
        //mEtContent = (EditText) findViewById(R.id.EditTextContent);
        mTvSource = (TextView)findViewById(R.id.textViewSource);
        mTvSource.setTextColor(Color.GRAY);
        mTvTarget = (TextView)findViewById(R.id.textViewTarget);
        mTvTarget.setTextColor(Color.GREEN);

        mTvTarget.setText(resortTable(NotExistSoundList));
        mBtnFile.setOnClickListener(mBtnOCL);
        mBtnOpen.setOnClickListener(mBtnOCL);
        mBtnNormal.setOnClickListener(mBtnOCL);
        mBtnQ5.setOnClickListener(mBtnOCL);
        mBtnQ6.setOnClickListener(mBtnOCL);
        mBtnQ22.setOnClickListener(mBtnOCL);
        mBtnQ27.setOnClickListener(mBtnOCL);
        mBtnBatchSave.setOnClickListener(mBtnOCL);
    }

    private View.OnClickListener mBtnOCL = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v.getId()==mBtnFile.getId()){
                //showDialog(OPEN_FILE_DIALOG_ID);
                createFilePopupWindow();
            }else if(v.getId() == mBtnOpen.getId()){
                showDialog(OPEN_FILE_DIALOG_ID);
            }else if(v.getId() == mBtnNormal.getId()){
                mTvTarget.setText(highLightWord(convertTextFile(currentFilePath)));

            }else if(v.getId() == mBtnBatchSave.getId()){
                //saveFile();
                //
                //batchConvertFiles(OPEN_PATH);
                mProgressDialog = new ProgressDialog(MainActivity.this);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        batchConvertFiles(OPEN_PATH);
                    }
                }).start();

            }else if(v.getId() == mBtnQ5.getId()){
                mTvTarget.setText(highLightWord(convertTextFileQ5(currentFilePath)));
            }else if(v.getId() == mBtnQ6.getId()){
                mTvTarget.setText(highLightWord(convertTextFileQ6(currentFilePath)));
            }else if(v.getId() == mBtnQ22.getId()){
                mTvTarget.setText(highLightWord(convertTextFileQ22(currentFilePath)));
            }else if(v.getId() == mBtnQ27.getId()){
                mTvTarget.setText(highLightWord(convertTextFileQ27(currentFilePath)));
            }
            /*
            if( !(v instanceof EditText)){
                InputMethodManager manager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0);
            }*/
        }
    };

    private void batchConvertFiles(String path){
        //List<File> list = null;
        StringBuilder stringBuilder = new StringBuilder();

            File[] files = null;
            try{
                files = new File(path).listFiles();
            }
            catch(Exception e){
                files = null;
            }
            if(files==null){
                // 璁块棶鍑洪敊
                Toast.makeText(this, "No rights to access!", Toast.LENGTH_SHORT).show();
                return;
            }
        /*
            if(list != null){
                list.clear();
            }
            else{
                list = new ArrayList<File>(files.length);
            }
            */
        int totalCnt = 0;
        int successCnt = 0;

            for(File file: files){
                Message msg = new Message();
                msg.what = MSG_PROGRESS_DIALOG;
                msg.arg1 = files.length;
                msg.arg2 = totalCnt;
                mHandler.sendMessage(msg);
                if(file.isFile() && file.getName().toLowerCase().contains("txt")){
                    totalCnt++;
                    //mTvSource.setText(Integer.toString(totalCnt) + "/" + Integer.toString(files.length));
                    String newFileName = file.getName().substring(0,file.getName().indexOf('.'));
                    String suffix = file.getName().substring(file.getName().indexOf('.'),file.getName().length());
                    String newPath = SAVE_PATH+"_"+newFileName+suffix;
                    if(convertFile(file,newPath))
                        successCnt++;
                        stringBuilder.append(newPath).append("\n");
                    }
            }
        stringBuilder.append(Integer.toString(successCnt) + "/" + Integer.toString(totalCnt) + " converted.\n");
        Message msg = new Message();
        msg.what = MSG_PROGRESS_DIALOG_DISSMISS;
        Bundle data = new Bundle();
        data.putString(EXTRA_STRING,stringBuilder.toString());
        msg.setData(data);
        mHandler.sendMessage(msg);

        //mTvSource.setText(stringBuilder.toString());

    }
    private boolean convertFile(File file, String path){

        try {
            FileOutputStream out = new FileOutputStream(new File(path));
            byte[] data = convertTextFile(file).toString().getBytes("UTF-8");
            if(data.length>0){
                //Toast.makeText(getApplicationContext(),"Saved to "+path,Toast.LENGTH_LONG).show();
                out.write(data);
                Log.v(TAG,"Saved to "+path);
            }else{
                Log.v(TAG,"Failed save to "+path);
            }
            out.close();
        } catch (IOException e) {
            Log.v(TAG,"Failed save to "+path);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void saveFile(String path){
        try {
            FileOutputStream out = new FileOutputStream(new File(path));
            byte[] data = mTvTarget.getText().toString().getBytes("UTF-8");
            if(data.length>0){
                //Toast.makeText(getApplicationContext(),"Saved to "+path,Toast.LENGTH_LONG).show();
                out.write(data);
                Log.v(TAG,"Saved to "+path);
            }else{
                Log.v(TAG,"Failed save to "+path);
            }
            out.close();
        } catch (IOException e) {
            Log.v(TAG,"Failed save to "+path);
            e.printStackTrace();
        }
    }

    private void saveFile(){
        String newFileName = currentFileName.substring(0,currentFileName.indexOf('.'));
        String suffix = currentFileName.substring(currentFileName.indexOf('.'),currentFileName.length());
        String path = SAVE_PATH+"_"+newFileName+suffix;
        try {
            FileOutputStream out = new FileOutputStream(new File(path));
            byte[] data = mTvTarget.getText().toString().getBytes("UTF-8");
            if(data.length>0){
                Toast.makeText(getApplicationContext(),"Saved to "+path,Toast.LENGTH_LONG).show();
                out.write(data);
            }else{
                Toast.makeText(getApplicationContext(),"Save failed",Toast.LENGTH_LONG).show();
            }
            out.close();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),"Save failed",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void createFilePopupWindow(){
        PopupMenu popupMenu = new PopupMenu(getApplicationContext(),mBtnFile);
        popupMenu.inflate(R.menu.file_popupmenu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.file_menu_open:
                        showDialog(OPEN_FILE_DIALOG_ID);
                        break;
                    case R.id.file_menu_save:
                        String str = mTvTarget.getText().toString();
                        if(mTvTarget.getText().toString() != null || mTvTarget.getText().toString().length() > 0){
                            showDialog(SAVE_FILE_DIALOG_ID);
                        }else{
                            Toast.makeText(getApplicationContext(),"Content is empty!",Toast.LENGTH_LONG).show();
                        }
                        break;
                    case R.id.file_menu_exit:
                        finish();
                        break;
                }
                return true;
            }
        });
        popupMenu.show();

    }

    private String resortTable(String[] table){
        String result="";
        for(int i=0;i<table.length;i++){
            if(i%16==0)result += "\n";
            result += "_"+table[i] + "_,";
        }

        return result;
    }

    private SpannableStringBuilder highLightWord(String str){
        SpannableStringBuilder builder = new SpannableStringBuilder(str);

        CharacterStyle span = null;
        Pattern p = Pattern.compile(FILTER_PATTERN);
        Matcher m = p.matcher(str);
        while (m.find()){
            span = new ForegroundColorSpan(Color.RED);
            builder.setSpan(span,m.start(),m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }
    private String convertTextFile(String path){
        File file = new File(path);
        return convertTextFile(file);
    }

    private String convertTextFileM2(String path){
        File file = new File(path);
        return convertTextFileM2(file);
    }
    private String convertTextFileQ5(String path){
        File file = new File(path);
        return convertTextFileQ5(file);
    }
    private String convertTextFileQ6(String path){
        File file = new File(path);
        return convertTextFileQ6(file);
    }
    private String convertTextFileQ22(String path){
        File file = new File(path);
        return convertTextFileQ22(file);
    }
    private String convertTextFileQ27(String path){
        File file = new File(path);
        return convertTextFileQ27(file);
    }
    private boolean contains(String str,String[] strTable){
        for(String strCmp:strTable){
            if(str.contains(strCmp)){
                return true;
            }
        }
        return false;
    }

    private String convertTextFile(File file){
        //File file = new File(path);
        StringBuilder stringbuilder = new StringBuilder();
        String string = null;
        int semicolonCnt  = 0;
        int partCnt = 0;
        int lineCnt = 0;
        int subLineCnt = 0;
        int lineOfSet = 0;
        BufferedReader reader;
        boolean isHelpExist = false;
        boolean isSubLine = false;
        Pattern pattern = Pattern.compile(FILTER_PATTERN);
        //Analysis
        try{
            reader = new BufferedReader(new FileReader(file));
            while((string=reader.readLine()) != null){
                if(!string.contains(";") && (string.indexOf(';')<5)){
                    semicolonCnt = 0;
                    int subLineIndex = string.indexOf(' ');
                    isSubLine = (subLineIndex<5 && subLineIndex>-1);
                    if(isSubLine){
                        subLineCnt ++;
                        if(subLineCnt == 2)isHelpExist=true;
                    }else{
                        subLineCnt=0;
                    }
                    lineCnt++;
                }else{
                    if(semicolonCnt == 0) {
                        if(partCnt==1){
                            lineOfSet = subLineCnt + 1;
                            firstlineData[5] = lineCnt;
                        }
                        partCnt++;
                        lineCnt = 0;
                    }
                    semicolonCnt++;
                }
            }
            //firstlineData[6] = lineCnt;
            reader.close();

        }catch(IOException e){
            e.printStackTrace();
            return null;
        }
        if(isHelpExist)firstlineData[4]=1;


        //process
        semicolonCnt  = 0;
        partCnt = 0;
        lineCnt = 0;
        subLineCnt = 0;
        isHelpExist = false;
        isSubLine = false;


        int readlineCnt = 0;
        String[] strSet = new String[lineOfSet];
        boolean[] isSublines = new boolean[lineOfSet];
        int subLineMax = 0;
        int[] subLineCnts = new int[lineOfSet];
        int pntStrSet = 0;
        boolean isLineValid = true;
        try{
            reader = new BufferedReader(new FileReader(file));
            while((string=reader.readLine()) != null){

                if(!string.contains(";") && (string.indexOf(';')<5)){
                    semicolonCnt = 0;
                    int wordCnt = 0;
                    StringBuilder b = new StringBuilder();
                    Matcher m = pattern.matcher(string);
                    String str = "";
                    int subLineIndex = string.indexOf(' ');
                    isSubLine = (subLineIndex<5 && subLineIndex>-1);
                    if(isSubLine){
                        subLineCnt ++;
                        if(subLineCnt == 2)isHelpExist=true;
                    }else{
                        subLineCnt=0;
                    }
                    while (m.find()){
                        str = string.substring(m.start(),m.end());
                        b.append(str);
                        wordCnt++;
                        if(wordCnt<4)b.append(USER_DIVIDER);
                    }
                    for(int i=wordCnt;i<4;i++){
                        b.append(USER_END_WORD);
                        if(i<3)b.append(USER_DIVIDER);
                    }


                    if(subLineCnt>subLineMax)subLineMax = subLineCnt;

                    strSet[pntStrSet] = b.toString();
                    isSublines[pntStrSet] = isSubLine;
                    subLineCnts[pntStrSet] = subLineCnt;
                    string = strSet[pntStrSet];
                    pntStrSet++;
                    if(pntStrSet>=strSet.length)pntStrSet=0;
                    //if(contains(string,NotExistSoundList))isLineValid = false;
                    if(partCnt == 1 && pntStrSet==0){
                        isLineValid = true;
                        for(int i=0;i<strSet.length;i++){
                            if(contains(strSet[i],NotExistSoundList))
                            {
                                isLineValid = false;
                                break;
                            }
                        }
                    }else{
                        isLineValid = !contains(string,NotExistSoundList);
                    }
                    if(isLineValid){
                        if(partCnt == 1){
                            if(pntStrSet == 0){
                                for(int i=0;i<lineOfSet;i++){
                                    lineCnt++;
                                    string = strSet[i];
                                    if(isSublines[i]){
                                        switch(subLineCnts[i]){
                                            case 1:
                                                stringbuilder.append("DW\t\t"+string+SUBLINE1_COMMENT+"\n");
                                                break;
                                            case 2:
                                                stringbuilder.append("DW\t\t"+string+SUBLINE2_COMMENT+"\n");
                                                break;
                                            default:
                                                stringbuilder.append("DW\t\t"+string+SUBLINE3_COMMENT+subLineCnts[i]+"\n");
                                        }
                                    }
                                    else stringbuilder.append("DW\t"+string+"\n");
                                }
                            }
                        }else{
                            lineCnt++;
                            stringbuilder.append("DW\t"+string+"\n");
                        }
                    }


                }else{
                    if(semicolonCnt == 0) {
                        stringbuilder.append(";Part\t" + partCnt+"\n");
                        if(partCnt==1){
                            lineOfSet = subLineCnt + 1;
                            firstlineData[5] = lineCnt;
                        }
                        partCnt++;
                        lineCnt = 0;
                    }
                    semicolonCnt++;
                }
            }
            firstlineData[6] = lineCnt;
            reader.close();

        }catch(IOException e){
            e.printStackTrace();
            return null;
        }

        String name = file.getName().toLowerCase();
        int tabIndex = 0;
        for(int i=0;i<MAX_QUESTION_NUM;i++){
            String str = UserTable.QUESION_NAME[i].toLowerCase();
            if(name.contains(str)){
                tabIndex = i;
                break;
            }
        }
        int Q_Range = UserTable.Q_RANGE[tabIndex];
        int Q_Type = UserTable.Q_TYPE[tabIndex];
        int Q_Type2 = UserTable.Q_TYPE2[tabIndex];
        int Q_TotalAns = UserTable.Q_TOTALANS[tabIndex];
        int Q_Help = UserTable.Q_HELP[tabIndex];
        firstlineData[0] = Q_Range;
        firstlineData[1] = Q_Type;
        firstlineData[2] = Q_Type2;
        firstlineData[3] = Q_TotalAns;
        firstlineData[4] = Q_Help;


        String result = "";

        //////////////////////////////////////////////
        //Header
        //////////////////////////////////////////////
        /**/
        result += ";" + file.getName() + "\n";
        for(int i=0;i<8;i++){
            result += ";" + FIRST_COMMENTS[i] +"\n";
            result += "DW\t" + Integer.toString(firstlineData[i]) + "\n";
        }
        //////////////////////////////////////////////
        result +=stringbuilder.toString();
        result += "\n//line of set is: "+lineOfSet;
        if(subLineMax >=3){
            result += "\n//subLineMax is: "+subLineMax;
        }

        return result;
    }
    private String convertTextFileM2(File file){
        //File file = new File(path);
        StringBuilder stringbuilder = new StringBuilder();
        //stringbuilder.append(FIRST_COMMENT);
        //stringbuilder.append(FIRST_8Bytes);
        String string = null;
        int semicolonCnt  = 0;
        int partCnt = 0;
        int lineCnt = 0;
        int subLineCnt = 0;
        int subLineMax = 0;
        BufferedReader reader;
        boolean isHelpExist = false;
        boolean isSubLine = false;

        Pattern pattern = Pattern.compile(FILTER_PATTERN);
        try{
            reader = new BufferedReader(new FileReader(file));
            while((string=reader.readLine()) != null){
                //System.out.println(str);

                //if(!string.contains(";")){
                if(!string.contains(";") && (string.indexOf(';')<5)){
                    semicolonCnt = 0;
                    int wordCnt = 0;
                    StringBuilder b = new StringBuilder();



                    Matcher m = pattern.matcher(string);
                    String str = "";
                    int subLineIndex = string.indexOf(' ');
                    isSubLine = (subLineIndex<5 && subLineIndex>-1);
                    if(isSubLine){
                        subLineCnt ++;
                        if(subLineCnt == 2)isHelpExist=true;
                    }else{
                        subLineCnt=0;
                    }

                    if(subLineCnt>subLineMax)subLineMax = subLineCnt;

                    while (m.find()){
                        str = string.substring(m.start(),m.end());
                        b.append(str);
                        wordCnt++;
                        if(wordCnt<4)b.append(USER_DIVIDER);
                    }
                    for(int i=wordCnt;i<4;i++){
                        b.append(USER_END_WORD);
                        if(i<3)b.append(USER_DIVIDER);
                    }

                    string = b.toString();
                    lineCnt++;

                    String lineStr = "";

                    if(isSubLine){
                        switch(subLineCnt){
                            case 1:
                                lineStr = "DW\t\t"+string+SUBLINE1_COMMENT+"\n";
                                break;
                            case 2:
                                lineStr = "DW\t\t"+string+SUBLINE2_COMMENT+"\n";
                                break;
                            default:
                                lineStr = "DW\t\t"+string+SUBLINE3_COMMENT+subLineCnt+"\n";
                        }

                    }
                    else lineStr = "DW\t"+string+"\n";
                    if(contains(string,NotExistSoundList)){
                        stringbuilder.append("\t;");
                        lineCnt--;
                    }
                    stringbuilder.append(lineStr);

                }else{
                    if(semicolonCnt == 0) {
                        stringbuilder.append(";Part\t" + partCnt+"\n");
                        if(partCnt==1)firstlineData[5] = lineCnt;
                        partCnt++;
                        lineCnt = 0;
                    }
                    semicolonCnt++;
                }
            }
            firstlineData[6] = lineCnt;
            reader.close();

        }catch(IOException e){
            e.printStackTrace();
            return null;
        }


        String name = file.getName().toLowerCase();
        int tabIndex = 0;
        for(int i=0;i<MAX_QUESTION_NUM;i++){
            String str = UserTable.QUESION_NAME[i].toLowerCase();
            if(name.contains(str)){
                tabIndex = i;
                break;
            }
        }
        int Q_Range = UserTable.Q_RANGE[tabIndex];
        int Q_Type = UserTable.Q_TYPE[tabIndex];
        int Q_Type2 = UserTable.Q_TYPE2[tabIndex];
        int Q_TotalAns = UserTable.Q_TOTALANS[tabIndex];
        int Q_Help = UserTable.Q_HELP[tabIndex];
        firstlineData[0] = Q_Range;
        firstlineData[1] = Q_Type;
        firstlineData[2] = Q_Type2;
        firstlineData[3] = Q_TotalAns;
        firstlineData[4] = Q_Help;


        String result = "";

        //////////////////////////////////////////////
        //Header
        //////////////////////////////////////////////
        /**/
        result += ";" + file.getName() + "\n";
        for(int i=0;i<8;i++){
            result += ";" + FIRST_COMMENTS[i] +"\n";
            result += "DW\t" + Integer.toString(firstlineData[i]) + "\n";
        }
        //////////////////////////////////////////////
        result +=stringbuilder.toString();

        if(subLineMax >=3){
            result += "\n//subLineMax is: "+subLineMax;
        }
        return result;
    }



    private String convertTextFileQ5(File file){

        String name = file.getName().toLowerCase();
        int tabIndex = 0;
        boolean tabFound = false;
        for(int i=0;i<MAX_QUESTION_NUM;i++){
            String str = UserTable.QUESION_NAME[i].toLowerCase();
            if(name.contains(str)){
                tabIndex = i;
                tabFound = true;
                break;
            }
        }
        int Q_Range = UserTable.Q_RANGE[tabIndex];
        int Q_Type = UserTable.Q_TYPE[tabIndex];
        int Q_Type2 = UserTable.Q_TYPE2[tabIndex];
        int Q_TotalAns = UserTable.Q_TOTALANS[tabIndex];
        int Q_Help = UserTable.Q_HELP[tabIndex];
        firstlineData[0] = 5;//Q_Range;
        firstlineData[1] = Q_Type;
        firstlineData[2] = Q_Type2;
        firstlineData[3] = Q_TotalAns;
        firstlineData[4] = Q_Help;

        String qName = "_"+ (tabFound?UserTable.QUESION_NAME[tabIndex]:"NIL");
        int qPart1Cnt = 0;
        List<String> labelList = new ArrayList<String>();


        StringBuilder stringbuilder = new StringBuilder();
        //stringbuilder.append(FIRST_COMMENT);
        //stringbuilder.append(FIRST_8Bytes);
        String string = null;
        int semicolonCnt  = 0;
        int partCnt = 0;
        int lineCnt = 0;
        int subLineCnt = 0;
        int subLineMax = 0;
        BufferedReader reader;
        boolean isHelpExist = false;
        boolean isSubLine = false;

        Pattern pattern = Pattern.compile(FILTER_PATTERN);
        try{
            reader = new BufferedReader(new FileReader(file));
            while((string=reader.readLine()) != null){
                //System.out.println(str);

                //if(!string.contains(";")){
                if(!string.contains(";") && (string.indexOf(';')<5)){
                    semicolonCnt = 0;
                    int wordCnt = 0;
                    StringBuilder b = new StringBuilder();



                    Matcher m = pattern.matcher(string);
                    String str = "";
                    int subLineIndex = string.indexOf(' ');
                    isSubLine = (subLineIndex<5 && subLineIndex>-1);
                    if(isSubLine){
                        subLineCnt ++;
                        if(subLineCnt == 2)isHelpExist=true;
                    }else{
                        subLineCnt=0;
                    }

                    if(subLineCnt>subLineMax)subLineMax = subLineCnt;

                    while (m.find()){
                        str = string.substring(m.start(),m.end());
                        b.append(str);
                        wordCnt++;
                        if(wordCnt<4)b.append(USER_DIVIDER);
                    }
                    for(int i=wordCnt;i<4;i++){
                        b.append(USER_END_WORD);
                        if(i<3)b.append(USER_DIVIDER);
                    }

                    string = b.toString();
                    lineCnt++;

                    String lineStr = "";

                    if(contains(string,NotExistSoundList)){
                        lineStr += ";";
                        lineCnt--;
                    }

                    if(isSubLine){
                        switch(subLineCnt){
                            case 1:
                                lineStr += "DW\t\t"+string+SUBLINE2_COMMENT+"\n";
                                break;
                            default:
                                lineStr += "DW\t\t"+string+SUBLINE3_COMMENT+subLineCnt+"\n";
                        }

                    }
                    else {
                        if(partCnt == 1){
                            stringbuilder.append("DW\t"+USER_END_WORD+"\n");
                            String label = qName+"_"+qPart1Cnt;
                            labelList.add(label);
                            //lineStr += label+":" + "\nDW\t"+string+"\n";
                            stringbuilder.append(label+":" + "\n");
                            qPart1Cnt++;
                        }else{

                        }
                        lineStr += "DW\t"+string+"\n";
                    }
                    stringbuilder.append(lineStr);

                }else{
                    if(semicolonCnt == 0) {
                        if(partCnt == 1){
                            stringbuilder.append("DW\t"+USER_END_WORD+"\n");
                            String label = qName+"_WRONG_ANS";
                            labelList.add(label);
                            stringbuilder.append(label+":\n");
                        }
                        stringbuilder.append(";Part\t" + partCnt+"\n");
                        //if(partCnt==1)firstlineData[5] = lineCnt;
                        partCnt++;
                        lineCnt = 0;
                    }
                    semicolonCnt++;
                }
            }
            firstlineData[5] = labelList.size()-1;
            firstlineData[6] = lineCnt;
            reader.close();

        }catch(IOException e){
            e.printStackTrace();
            return null;
        }




        String result = "";

        //////////////////////////////////////////////
        //Header
        //////////////////////////////////////////////
        /**/
        result += ";" + file.getName() + "\n";
        for(int i=0;i<8;i++){
            result += ";" + FIRST_COMMENTS[i] +"\n";
            result += "DW\t" + Integer.toString(firstlineData[i]) + "\n";
        }
        //////////////////////////////////////////////
        //Address
        result += ";Question Address:\n";
        for(String label:labelList){
            result += "DW\t" + label + "$L,\t" + label + "$M,\t" +label + "$H\n";
        }
        //////////////////////////////////////////////
        //Contents
        result +=stringbuilder.toString();
        //////////////////////////////////////////////

        if(subLineMax >=3){
            result += "\n//subLineMax is: "+subLineMax;
        }
        return result;
    }

    private String convertTextFileQ6(File file){


        String name = file.getName().toLowerCase();
        int tabIndex = 0;
        boolean tabFound = false;
        for(int i=0;i<MAX_QUESTION_NUM;i++){
            String str = UserTable.QUESION_NAME[i].toLowerCase();
            if(name.contains(str)){
                tabIndex = i;
                tabFound = true;
                break;
            }
        }
        int Q_Range = UserTable.Q_RANGE[tabIndex];
        int Q_Type = UserTable.Q_TYPE[tabIndex];
        int Q_Type2 = UserTable.Q_TYPE2[tabIndex];
        int Q_TotalAns = UserTable.Q_TOTALANS[tabIndex];
        int Q_Help = UserTable.Q_HELP[tabIndex];
        firstlineData[0] = 6;//Q_Range;
        firstlineData[1] = Q_Type;
        firstlineData[2] = Q_Type2;
        firstlineData[3] = Q_TotalAns;
        firstlineData[4] = Q_Help;

        String qName = "_"+ (tabFound?UserTable.QUESION_NAME[tabIndex]:"NIL");
        int qPart1Cnt = 0;
        List<String> labelList = new ArrayList<String>();



        StringBuilder stringbuilder = new StringBuilder();
        String string = null;
        int semicolonCnt  = 0;
        int partCnt = 0;
        int lineCnt = 0;
        int subLineCnt = 0;
        int lineOfSet = 0;
        BufferedReader reader;
        boolean isHelpExist = false;
        boolean isSubLine = false;
        Pattern pattern = Pattern.compile(FILTER_PATTERN);
        //Analysis
        try{
            reader = new BufferedReader(new FileReader(file));
            while((string=reader.readLine()) != null){
                if(!string.contains(";") && (string.indexOf(';')<5)){
                    semicolonCnt = 0;
                    int subLineIndex = string.indexOf(' ');
                    isSubLine = (subLineIndex<5 && subLineIndex>-1);
                    if(isSubLine){
                        subLineCnt ++;
                        if(subLineCnt == 2)isHelpExist=true;
                    }else{
                        subLineCnt=0;
                    }
                    lineCnt++;
                }else{
                    if(semicolonCnt == 0) {
                        if(partCnt==1){
                            lineOfSet = subLineCnt + 1;
                        }
                        partCnt++;
                        lineCnt = 0;
                    }
                    semicolonCnt++;
                }
            }
            //firstlineData[6] = lineCnt;
            reader.close();

        }catch(IOException e){
            e.printStackTrace();
            return null;
        }
        if(isHelpExist)firstlineData[4]=1;


        //process
        semicolonCnt  = 0;
        partCnt = 0;
        lineCnt = 0;
        subLineCnt = 0;
        isHelpExist = false;
        isSubLine = false;


        int readlineCnt = 0;
        String[] strSet = new String[lineOfSet];
        boolean[] isSublines = new boolean[lineOfSet];
        int subLineMax = 0;
        int[] subLineCnts = new int[lineOfSet];
        int pntStrSet = 0;
        boolean isLineValid = true;
        try{
            reader = new BufferedReader(new FileReader(file));
            while((string=reader.readLine()) != null){

                if(!string.contains(";") && (string.indexOf(';')<5)){
                    semicolonCnt = 0;
                    int wordCnt = 0;
                    StringBuilder b = new StringBuilder();
                    Matcher m = pattern.matcher(string);
                    String str = "";
                    int subLineIndex = string.indexOf(' ');
                    isSubLine = (subLineIndex<5 && subLineIndex>-1);
                    if(isSubLine){
                        subLineCnt ++;
                        if(subLineCnt == 2)isHelpExist=true;
                    }else{
                        subLineCnt=0;
                    }
                    while (m.find()){
                        str = string.substring(m.start(),m.end());
                        b.append(str);
                        wordCnt++;
                        if(wordCnt<4)b.append(USER_DIVIDER);
                    }
                    if(!isSubLine){
                        for(int i=wordCnt;i<4;i++){
                            b.append(USER_END_WORD);
                            if(i<3)b.append(USER_DIVIDER);
                        }
                    }else{
                        b.append(string);
                    }


                    if(subLineCnt>subLineMax)subLineMax = subLineCnt;

                    strSet[pntStrSet] = b.toString();
                    isSublines[pntStrSet] = isSubLine;
                    subLineCnts[pntStrSet] = subLineCnt;
                    string = strSet[pntStrSet];
                    pntStrSet++;
                    if(pntStrSet>=strSet.length)pntStrSet=0;
                    //if(contains(string,NotExistSoundList))isLineValid = false;
                    if(partCnt == 1 && pntStrSet==0){
                        isLineValid = true;
                        for(int i=0;i<strSet.length;i++){
                            if(contains(strSet[i],NotExistSoundList))
                            {
                                isLineValid = false;
                                break;
                            }
                        }
                    }else{
                        isLineValid = !contains(string,NotExistSoundList);
                    }
                    if(isLineValid){
                        if(partCnt == 1){
                            if(pntStrSet == 0){
                                for(int i=0;i<lineOfSet;i++){
                                    string = strSet[i];
                                    if(isSublines[i]){
                                        switch(subLineCnts[i]){
                                            case 1:
                                                stringbuilder.append("DW\t\t"+string+SUBLINE1_COMMENT+"\n");
                                                break;
                                            case 2:
                                                stringbuilder.append("DW\t\t"+string+SUBLINE2_COMMENT+"\n");
                                                break;
                                            default:
                                                stringbuilder.append("DW\t\t"+string+SUBLINE3_COMMENT+subLineCnts[i]+"\n");
                                        }
                                    }
                                    else {
                                        lineCnt++;
                                        stringbuilder.append("DW\t"+string+"\n");
                                    }
                                }
                            }
                        }else{
                            lineCnt++;
                            stringbuilder.append("DW\t"+string+"\n");
                        }
                    }


                }else{
                    if(semicolonCnt == 0) {
                        stringbuilder.append(";Part\t" + partCnt+"\n");
                        if(partCnt==1){
                            lineOfSet = subLineCnt + 1;
                            firstlineData[5] = lineCnt;
                        }
                        partCnt++;
                        lineCnt = 0;
                    }
                    semicolonCnt++;
                }
            }
            firstlineData[6] = lineCnt;
            reader.close();

        }catch(IOException e){
            e.printStackTrace();
            return null;
        }

        String result = "";

        //////////////////////////////////////////////
        //Header
        //////////////////////////////////////////////
        /**/
        result += ";" + file.getName() + "\n";
        for(int i=0;i<8;i++){
            result += ";" + FIRST_COMMENTS[i] +"\n";
            result += "DW\t" + Integer.toString(firstlineData[i]) + "\n";
        }
        //////////////////////////////////////////////
        result +=stringbuilder.toString();
        result += "\n//line of set is: "+lineOfSet;
        if(subLineMax >=3){
            result += "\n//subLineMax is: "+subLineMax;
        }

        return result;
    }

    private String convertTextFileQ22(File file){

        String name = file.getName().toLowerCase();
        int tabIndex = 0;
        boolean tabFound = false;
        for(int i=0;i<MAX_QUESTION_NUM;i++){
            String str = UserTable.QUESION_NAME[i].toLowerCase();
            if(name.contains(str)){
                tabIndex = i;
                tabFound = true;
                break;
            }
        }
        int Q_Range = UserTable.Q_RANGE[tabIndex];
        int Q_Type = UserTable.Q_TYPE[tabIndex];
        int Q_Type2 = UserTable.Q_TYPE2[tabIndex];
        int Q_TotalAns = UserTable.Q_TOTALANS[tabIndex];
        int Q_Help = UserTable.Q_HELP[tabIndex];
        firstlineData[0] = 22;//Q_Range;
        firstlineData[1] = Q_Type;
        firstlineData[2] = Q_Type2;
        firstlineData[3] = Q_TotalAns;
        firstlineData[4] = Q_Help;

        String qName = "_"+ (tabFound?UserTable.QUESION_NAME[tabIndex]:"NIL");

        List<String> labelList = new ArrayList<String>();
        List<String> mainList = new ArrayList<String>();
        List<List<String>> subList = new ArrayList<List<String>>();
        List<String> subListContent;
        List<String> wrongList = new ArrayList<String>();
        List<boolean[]> validLineList = new ArrayList<boolean[]>();


        //StringBuilder stringbuilder = new StringBuilder();

        String lineString = null;
        String mainString = "";
        String subString = "";
        int semicolonCnt  = 0;
        int partCnt = 0;
        int lineCnt = 0;
        BufferedReader reader;
        boolean isSubLine = false;

        Pattern pattern = Pattern.compile(FILTER_PATTERN);
        try{
            reader = new BufferedReader(new FileReader(file));
            while((lineString=reader.readLine()) != null){
                if(!lineString.contains(";") && (lineString.indexOf(';')<5)){
                    semicolonCnt = 0;

                    Matcher m = pattern.matcher(lineString);
                    String str = "";
                    StringBuilder b = new StringBuilder();

                    int wordCnt = 0;
                    while (m.find()){
                        str = lineString.substring(m.start(),m.end());
                        b.append(str);
                        wordCnt++;
                        if(wordCnt<4)b.append(USER_DIVIDER);
                    }
                    for(int i=wordCnt;i<4;i++){
                        b.append(USER_END_WORD);
                        if(i<3)b.append(USER_DIVIDER);
                    }


                    boolean isLineValid = !contains(lineString,NotExistSoundList);

                    int subLineIndex = lineString.indexOf(' ');
                    isSubLine = (subLineIndex<5 && subLineIndex>-1);

                    if(isSubLine){
                        if(isLineValid){
                            subString = "DW\t\t";
                            subString += b.toString();
                            if(partCnt==1){
                                subList.get(subList.size()-1).add(subString);
                            }
                        }/*else{
                            subString = ";\tDW\t\t";
                        }*/
                    }else{
                        if(isLineValid){
                            mainString = "DW\t";
                        }else{
                            mainString = ";\tDW\t";
                        }
                        mainString += b.toString();
                        if(partCnt==1){
                            String label = qName+"_"+mainList.size();
                            labelList.add(label);
                            subListContent = new ArrayList<String>();
                            mainList.add(mainString);
                            subList.add(subListContent);
                            validLineList.add(new boolean[]{isLineValid});
                        }else{
                            if(isLineValid)
                                wrongList.add(mainString);
                        }
                    }
                }else{
                    if(semicolonCnt == 0) {
                        if(partCnt == 1){
                            String label = qName+"_WRONG_ANS";
                            labelList.add(label);
                        }
                        partCnt++;
                        lineCnt = 0;
                    }
                    semicolonCnt++;
                }

            }
            firstlineData[5] = labelList.size()-1;
            firstlineData[6] = wrongList.size();
            reader.close();

        }catch(IOException e){
            e.printStackTrace();
            return null;
        }
        String result = "";


        //////////////////////////////////////////////
        //Header
        //////////////////////////////////////////////
        /**/
        result += ";" + file.getName() + "\n";
        for(int i=0;i<8;i++){
            result += ";" + FIRST_COMMENTS[i] +"\n";
            result += "DW\t" + Integer.toString(firstlineData[i]) + "\n";
        }
        //////////////////////////////////////////////
        //Address
        result += ";Question Address:\n";
        for(String label:labelList){
            result += "DW\t" + label + "$L,\t" + label + "$M,\t" +label + "$H\n";
        }
        //////////////////////////////////////////////
        //Contents
        StringBuilder contentString = new StringBuilder();
        for(int i=0;i<mainList.size();i++){
            String validSign = validLineList.get(i)[0]?"":";";
            contentString.append(labelList.get(i)).append(":\n");
            contentString.append(validSign).append(mainList.get(i)).append("\n");
            contentString.append(validSign).append("\tDW\t" +subList.get(i).size()).append(","+USER_END_WORD).append(","+USER_END_WORD).append(","+USER_END_WORD).append(";Counts\n");
            for(int j=0;j<subList.get(i).size();j++){
                contentString.append(validSign).append(subList.get(i).get(j)).append(";"+j+"\n");
            }
        }
        contentString.append(labelList.get(labelList.size()-1)).append(":\n");
        for(String str:wrongList){
            contentString.append(str).append("\n");
        }
        result +=contentString.toString();


        //////////////////////////////////////////////
        /*
        if(subLineMax >=3){
            result += "\n//subLineMax is: "+subLineMax;
        }
        */
        return result;
    }

    private String convertTextFileQ27(File file){

        String name = file.getName().toLowerCase();
        int tabIndex = 0;
        boolean tabFound = false;
        for(int i=0;i<MAX_QUESTION_NUM;i++){
            String str = UserTable.QUESION_NAME[i].toLowerCase();
            if(name.contains(str)){
                tabIndex = i;
                tabFound = true;
                break;
            }
        }
        int Q_Range = UserTable.Q_RANGE[tabIndex];
        int Q_Type = UserTable.Q_TYPE[tabIndex];
        int Q_Type2 = UserTable.Q_TYPE2[tabIndex];
        int Q_TotalAns = UserTable.Q_TOTALANS[tabIndex];
        int Q_Help = UserTable.Q_HELP[tabIndex];
        firstlineData[0] = 27;//Q_Range;
        firstlineData[1] = Q_Type;
        firstlineData[2] = Q_Type2;
        firstlineData[3] = Q_TotalAns;
        firstlineData[4] = Q_Help;

        String qName = "_"+ (tabFound?UserTable.QUESION_NAME[tabIndex]:"NIL");

        List<String> labelList = new ArrayList<String>();
        List<String> mainList = new ArrayList<String>();
        List<List<String>> subList = new ArrayList<List<String>>();
        List<String> subListContent;
        List<String> wrongList = new ArrayList<String>();
        List<boolean[]> validLineList = new ArrayList<boolean[]>();


        //StringBuilder stringbuilder = new StringBuilder();

        String lineString = null;
        String mainString = "";
        String subString = "";
        int semicolonCnt  = 0;
        int partCnt = 0;
        int lineCnt = 0;
        BufferedReader reader;
        boolean isSubLine = false;

        Pattern pattern = Pattern.compile(FILTER_PATTERN);
        Pattern patternNum = Pattern.compile(NUMBER_PATTERN);
        try{
            reader = new BufferedReader(new FileReader(file));
            while((lineString=reader.readLine()) != null){
                if(!lineString.contains(";") && (lineString.indexOf(';')<5)){
                    semicolonCnt = 0;

                    Matcher m = pattern.matcher(lineString);
                    Matcher numMatcher = patternNum.matcher(lineString);
                    String str = "";
                    StringBuilder b = new StringBuilder();
                    StringBuilder nSB = new StringBuilder();

                    int wordCnt = 0;
                    while (m.find()){
                        str = lineString.substring(m.start(),m.end());
                        b.append(str);
                        wordCnt++;
                        if(wordCnt<4)b.append(USER_DIVIDER);
                    }
                    if(wordCnt>0){
                        for(int i=wordCnt;i<4;i++){
                            b.append(USER_END_WORD);
                            if(i<3)b.append(USER_DIVIDER);
                        }
                    }else{
                        while (numMatcher.find()){
                            str = lineString.substring(numMatcher.start(),numMatcher.end());
                            nSB.append(str).append(",\t");
                        }
                        for(int i=1;i<4;i++){
                            nSB.append(USER_END_WORD);
                            if(i<3)nSB.append(USER_DIVIDER);
                        }
                    }



                    boolean isLineValid = !contains(lineString,NotExistSoundList);

                    int subLineIndex = lineString.indexOf(' ');
                    isSubLine = (subLineIndex<5 && subLineIndex>-1);

                    if(isSubLine){
                        if(isLineValid){
                            subString = "DW\t\t";
                            if(wordCnt>0){
                                subString += b.toString();
                            }else{
                                subString += nSB.toString();
                            }
                            if(partCnt==1){
                                subList.get(subList.size()-1).add(subString);
                            }
                        }/*else{
                            subString = ";\tDW\t\t";
                        }*/
                    }else{
                        if(isLineValid){
                            mainString = "DW\t";
                        }else{
                            mainString = ";\tDW\t";
                        }
                        mainString += b.toString();
                        if(partCnt==1){
                            String label = qName+"_"+mainList.size();
                            labelList.add(label);
                            subListContent = new ArrayList<String>();
                            mainList.add(mainString);
                            subList.add(subListContent);
                            validLineList.add(new boolean[]{isLineValid});
                        }else{
                            if(isLineValid)
                                wrongList.add(mainString);
                        }
                    }
                }else{
                    if(semicolonCnt == 0) {
                        if(partCnt == 1){
                            String label = qName+"_WRONG_ANS";
                            labelList.add(label);
                        }
                        partCnt++;
                        lineCnt = 0;
                    }
                    semicolonCnt++;
                }

            }
            firstlineData[5] = labelList.size()-1;
            firstlineData[6] = wrongList.size();
            reader.close();

        }catch(IOException e){
            e.printStackTrace();
            return null;
        }
        String result = "";


        //////////////////////////////////////////////
        //Header
        //////////////////////////////////////////////
        /**/
        result += ";" + file.getName() + "\n";
        for(int i=0;i<8;i++){
            result += ";" + FIRST_COMMENTS[i] +"\n";
            result += "DW\t" + Integer.toString(firstlineData[i]) + "\n";
        }
        //////////////////////////////////////////////
        //Address
        result += ";Question Address:\n";
        for(String label:labelList){
            result += "DW\t" + label + "$L,\t" + label + "$M,\t" +label + "$H\n";
        }
        //////////////////////////////////////////////
        //Contents
        StringBuilder contentString = new StringBuilder();
        for(int i=0;i<mainList.size();i++){
            String validSign = validLineList.get(i)[0]?"":";";
            contentString.append(labelList.get(i)).append(":\n");
            contentString.append(validSign).append(mainList.get(i)).append("\n");
            contentString.append(validSign).append("\tDW\t" +subList.get(i).size()).append(","+USER_END_WORD).append(","+USER_END_WORD).append(","+USER_END_WORD).append(";Counts\n");
            for(int j=0;j<subList.get(i).size();j++){
                contentString.append(validSign).append(subList.get(i).get(j)).append(";"+j+"\n");
            }
        }
        contentString.append(labelList.get(labelList.size()-1)).append(":\n");
        for(String str:wrongList){
            contentString.append(str).append("\n");
        }
        result +=contentString.toString();


        //////////////////////////////////////////////
        /*
        if(subLineMax >=3){
            result += "\n//subLineMax is: "+subLineMax;
        }
        */
        return result;
    }
    private String readTxtFile(String path){
        File file = new File(path);
        String  str = null;
        try {
            FileInputStream inputStream = new FileInputStream(file);
            int length = inputStream.available();
            byte[] buffer = new byte[length];
            while(inputStream.read(buffer) !=-1);
            inputStream.close();
            str = new String(buffer,"UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return str;
    }




    @Override
    protected Dialog onCreateDialog(int id) {
        if(id == OPEN_FILE_DIALOG_ID){
            Map<String,Integer> images = new HashMap<String, Integer>();
            images.put(OpenFileDialog.sParent,R.drawable.filedialog_folder_up);
            images.put(OpenFileDialog.sRoot,R.drawable.filedialog_root);
            images.put(OpenFileDialog.sFolder,R.drawable.filedialog_folder);
            images.put("wav",R.drawable.filedialog_wavfile);
            images.put(OpenFileDialog.sEmpty,R.drawable.filedialog_file);
            //images.put("bin",R.drawable.filedialog_file);
            Dialog dialog = OpenFileDialog.createDialog(id,this,"Open File",new CallbackBundle(){

                        @Override
                        public void callback(Bundle bundle) {
                            currentFilePath = bundle.getString(OpenFileDialog.EXTRA_STRING_PATH);
                            currentFileName = bundle.getString(OpenFileDialog.EXTRA_STRING_NAME);
                            Log.v(TAG,"path= "+ currentFilePath);
                            Log.v(TAG,"currentFileName= "+ currentFileName);
                            //currentPath = filepath.substring(0,filepath.lastIndexOf("/")+1);
                            String txt = readTxtFile(currentFilePath);

                            if(txt != null){
                                mTvSource.setText(txt);
                            }
                        }
                    },"",images,OPEN_PATH
            );
            return dialog;
        }else if(id == SAVE_FILE_DIALOG_ID){
            Map<String,Integer> images = new HashMap<String, Integer>();
            images.put(SaveFileDialog.sParent,R.drawable.filedialog_folder_up);
            images.put(SaveFileDialog.sRoot,R.drawable.filedialog_root);
            images.put(SaveFileDialog.sFolder,R.drawable.filedialog_folder);
            images.put("wav",R.drawable.filedialog_wavfile);
            images.put(SaveFileDialog.sEmpty,R.drawable.filedialog_file);
            //images.put("bin",R.drawable.filedialog_file);
            String newFileName = currentFileName.substring(0,currentFileName.indexOf('.'));
            String suffix = currentFileName.substring(currentFileName.indexOf('.'),currentFileName.length());
            Dialog dialog = SaveFileDialog.createDialog(id,this,"Save File",new CallbackBundle(){
                        @Override
                        public void callback(Bundle bundle) {
                            String path = bundle.getString(SaveFileDialog.EXTRA_STRING_PATH);
                            try {
                                FileOutputStream out = new FileOutputStream(new File(path));
                                byte[] data = mTvTarget.getText().toString().getBytes("UTF-8");
                                if(data.length>0){
                                    Toast.makeText(getApplicationContext(),"Saved",Toast.LENGTH_LONG).show();
                                    out.write(data);
                                }else{
                                    Toast.makeText(getApplicationContext(),"Save failed",Toast.LENGTH_LONG).show();
                                }
                                out.close();
                            } catch (IOException e) {
                                Toast.makeText(getApplicationContext(),"Save failed",Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                        }
                    },"",images,SAVE_PATH
            );
            return dialog;

        }
        return null;
    }
}
