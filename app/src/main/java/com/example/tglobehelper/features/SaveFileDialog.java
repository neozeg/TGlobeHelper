package com.example.tglobehelper.features;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SimpleAdapter;
import android.widget.Toast;


import com.example.tglobehelper.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2016/6/30.
 */
public class SaveFileDialog {
    public static String TAG = "SaveFileDialog";
    static final public String EXTRA_STRING_NAME = "extra.string.name";
    static final public String EXTRA_STRING_PATH = "extra.string.path";
    static final public String EXTRA_STRING_IMG = "extra.string.img";
    static final public String sRoot = "/sdcard";
    static final public String sParent = "..";
    static final public String sFolder = ".";
    static final public String sEmpty = "";
    static final private String sOnErrorMsg = "No rights to access!";
/*
    public static Dialog createDialog(int id, Context context, String title, CallbackBundle callback, String suffix, Map<String, Integer> images){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(new FileSelectView(context, id, callback, suffix, images));
        Dialog dialog = builder.create();
        //dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setTitle(title);
        return dialog;
    }
    */
    public static Dialog createDialog(int id, final Context context, String title, CallbackBundle callback, String suffix, Map<String, Integer> images, String path){
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        //builder.setView(new FileSelectView(context, id, callback, suffix, images,path));
        final FileSelectView fileSelectView = new FileSelectView(context, id, callback, suffix, images,path);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_savefile,null);
        final ViewHolder holder = new ViewHolder();

        holder.etPath =(EditText) view.findViewById(R.id.editTextSavePath);
        holder.lyFileList = (LinearLayout) view.findViewById(R.id.LinearLayoutFileList);
        holder.lyFileList.addView(fileSelectView);
        holder.mBtnSave = (Button) view.findViewById(R.id.buttonDialogSave);
        holder.mBtnCancel = (Button) view.findViewById(R.id.buttonDialogCancel);
        fileSelectView.setPathEditText(holder.etPath);

        view.setTag(holder);

        builder.setView(view);
        final Dialog dialog = builder.create();
        //dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setTitle(title);
        holder.mBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        holder.mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileSelectView.checkFilePathValid(holder.etPath.getText().toString());
            }
        });
        return dialog;
    }



    static  private class ViewHolder{
        public EditText etPath;
        public LinearLayout lyFileList;
        public Button mBtnSave,mBtnCancel;
    }

    static class FileSelectView extends ListView implements AdapterView.OnItemClickListener {

        private Context mContext;
        private CallbackBundle callback = null;
        private String path = sRoot;
        private List<Map<String, Object>> list = null;
        private int dialogid = 0;

        private String suffix = null;

        private Map<String, Integer> imagemap = null;

        private EditText etPath;


/*
        public FileSelectView(Context context, int dialogid, CallbackBundle callback, String suffix, Map<String, Integer> images) {
            super(context);
            this.imagemap = images;
            this.suffix = suffix==null?"":suffix.toLowerCase();
            this.callback = callback;
            this.dialogid = dialogid;
            this.setOnItemClickListener(this);
            this.setOnItemLongClickListener(onItemLongClickListener);
            refreshFileList();
        }
*/
        public FileSelectView(Context context, int dialogid, CallbackBundle callback, String suffix, Map<String, Integer> images,String path) {
            super(context);
            mContext = context;
            this.imagemap = images;
            this.suffix = suffix==null?"":suffix.toLowerCase();
            this.callback = callback;
            this.dialogid = dialogid;
            this.setOnItemClickListener(this);
            this.setOnItemLongClickListener(onItemLongClickListener);
            this.path = path==null?sRoot:path;
            refreshFileList();
        }

        public void setPathEditText(EditText et){
            etPath = et;
            etPath.setText(path);
        }

        private String getSuffix(String filename){
            int dix = filename.lastIndexOf('.');
            if(dix<0){
                return "";
            }
            else{
                return filename.substring(dix+1);
            }
        }

        private int getImageId(String s){
            if(imagemap == null){
                return 0;
            }
            else if(imagemap.containsKey(s)){
                return imagemap.get(s);
            }
            else if(imagemap.containsKey(sEmpty)){
                return imagemap.get(sEmpty);
            }
            else {
                return 0;
            }
        }

        private int refreshFileList()
        {
            // 鍒锋柊鏂囦欢鍒楄〃
            File[] files = null;
            try{
                files = new File(path).listFiles();
            }
            catch(Exception e){
                files = null;
            }
            if(files==null){
                // 璁块棶鍑洪敊
                Toast.makeText(getContext(), sOnErrorMsg, Toast.LENGTH_SHORT).show();
                return -1;
            }
            if(list != null){
                list.clear();
            }
            else{
                list = new ArrayList<Map<String, Object>>(files.length);
            }

            // 鐢ㄦ潵鍏堜繚瀛樻枃浠跺す鍜屾枃浠跺す鐨勪袱涓垪琛�
            ArrayList<Map<String, Object>> lfolders = new ArrayList<Map<String, Object>>();
            ArrayList<Map<String, Object>> lfiles = new ArrayList<Map<String, Object>>();

            if(!this.path.equals(sRoot)){
                // 娣诲姞鏍圭洰褰� 鍜� 涓婁竴灞傜洰褰�
                Map<String, Object> map = new HashMap<String, Object>();
                map.put(EXTRA_STRING_NAME, sRoot);
                map.put(EXTRA_STRING_PATH, sRoot);
                map.put(EXTRA_STRING_IMG, getImageId(sRoot));
                list.add(map);

                map = new HashMap<String, Object>();
                map.put(EXTRA_STRING_NAME, sParent);
                map.put(EXTRA_STRING_PATH, path);
                map.put(EXTRA_STRING_IMG, getImageId(sParent));
                list.add(map);
            }

            for(File file: files)
            {
                if(file.isDirectory() && file.listFiles()!=null){
                    // 娣诲姞鏂囦欢澶�
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put(EXTRA_STRING_NAME, file.getName());
                    map.put(EXTRA_STRING_PATH, file.getPath());
                    map.put(EXTRA_STRING_IMG, getImageId(sFolder));
                    lfolders.add(map);
                }
                else if(file.isFile()){
                    // 娣诲姞鏂囦欢
                    String sf = getSuffix(file.getName()).toLowerCase();
                    if(suffix == null || suffix.length()==0 || (sf.length()>0 && suffix.indexOf("."+sf+";")>=0)){
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put(EXTRA_STRING_NAME, file.getName());
                        map.put(EXTRA_STRING_PATH, file.getPath());
                        map.put(EXTRA_STRING_IMG, getImageId(sf));
                        lfiles.add(map);
                    }
                }
            }

            list.addAll(lfolders); // 鍏堟坊鍔犳枃浠跺す锛岀‘淇濇枃浠跺す鏄剧ず鍦ㄤ笂闈�
            list.addAll(lfiles);	//鍐嶆坊鍔犳枃浠�


            SimpleAdapter adapter = new SimpleAdapter(getContext(), list, R.layout.filedialogitem, new String[]{EXTRA_STRING_IMG, EXTRA_STRING_NAME, EXTRA_STRING_PATH}, new int[]{R.id.filedialogitem_img, R.id.filedialogitem_name, R.id.filedialogitem_path});
            this.setAdapter(adapter);
            return files.length;
        }

        private void checkFilePathValid(String path){
            File file  = new File(path);
            if(file.isDirectory()){
                Toast.makeText(mContext,"Please input the file name",Toast.LENGTH_LONG).show();
            }else if(file.isFile()){
                //Toast.makeText(mContext,"File existed, Do you want to overwrite",Toast.LENGTH_LONG).show();
                final String filePath = path;
                final AlertDialog.Builder builder = new AlertDialog.Builder(mContext,android.R.style.Theme_Holo_Dialog);
                builder.setTitle("Do you want to overwrite the existed file?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((Activity)mContext).dismissDialog(dialogid);
                        Bundle bundle = new Bundle();
                        bundle.putString(EXTRA_STRING_PATH, filePath);
                        callback.callback(bundle);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.show();
            }else{
                ((Activity)mContext).dismissDialog(dialogid);
                Bundle bundle = new Bundle();
                bundle.putString(EXTRA_STRING_PATH, path);
                callback.callback(bundle);
            }
        }


        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            // 鏉＄洰閫夋嫨
            String pt = (String) list.get(position).get(EXTRA_STRING_PATH);
            String fn = (String) list.get(position).get(EXTRA_STRING_NAME);
            if(fn.equals(sRoot) || fn.equals(sParent)){
                // 濡傛灉鏄洿鐩綍鎴栬�呬笂涓�灞�
                File fl = new File(pt);
                String ppt = fl.getParent();
                if(ppt != null){
                    // 杩斿洖涓婁竴灞�
                    path = ppt;
                }
                else{
                    // 杩斿洖鏇寸洰褰�
                    path = sRoot;
                }
            }
            else{
                File fl = new File(pt);
                if(fl.isFile()){
                    // 濡傛灉鏄枃浠�
                    /*
                    ((Activity)getContext()).dismissDialog(this.dialogid); // 璁╂枃浠跺す瀵硅瘽妗嗘秷澶�

                    // 璁剧疆鍥炶皟鐨勮繑鍥炲��
                    Bundle bundle = new Bundle();
                    bundle.putString(EXTRA_STRING_PATH, pt);
                    bundle.putString(EXTRA_STRING_NAME, fn);
                    // 璋冪敤浜嬪厛璁剧疆鐨勫洖璋冨嚱鏁�
                    this.callback.callback(bundle);
                    */
                    etPath.setText(pt);
                    return;
                }
                else if(fl.isDirectory()){
                    // 濡傛灉鏄枃浠跺す
                    // 閭ｄ箞杩涘叆閫変腑鐨勬枃浠跺す
                    path = pt;
                }
            }
            if(etPath != null){
                etPath.setText(path);
            }
            this.refreshFileList();
        }
        private OnItemLongClickListener onItemLongClickListener = new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String pt = (String) list.get(position).get(EXTRA_STRING_PATH);
                File fl = new File(pt);
                if(fl.isDirectory()){
                    path = pt;
                    refreshFileList();
                    return true;
                }else if(fl.isFile()){
                    final File file = fl;
                    PopupMenu popupMenu = new PopupMenu(mContext,view);
                    popupMenu.inflate(R.menu.file_longclick_popupmenu);
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()){
                                case R.id.file_menu_delete:
                                    file.delete();
                                    refreshFileList();
                                    break;
                            }
                            return true;
                        }
                    });
                    popupMenu.show();
                    return true;
                }
                return false;
            }
        };
    }
}
