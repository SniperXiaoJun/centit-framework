package com.centit.framework.staticsystem.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.centit.framework.common.SysParametersUtils;
import com.centit.framework.model.adapter.PlatformEnvironment;
import com.centit.framework.staticsystem.po.*;
import com.centit.support.file.FileIOOpt;
import com.centit.support.file.FileSystemOpt;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class JsonPlatformEnvironment extends AbstractStaticPlatformEnvironment
    implements PlatformEnvironment {

    private static Log logger = LogFactory.getLog(JsonPlatformEnvironment.class);

    public void init(){
        reloadDictionary();
        reloadSecurityMetadata();
        //reloadIPEnvironmen();
    }


    public void loadConfigFromJSONString(String jsonStr){
        JSONObject json = JSON.parseObject(jsonStr);
        userinfos = JSON.parseArray(json.getString("userInfos"), UserInfo.class);
        optinfos = JSON.parseArray(json.getString("optInfos"), OptInfo.class);
        optmethods = JSON.parseArray(json.getString("optMethods"), OptMethod.class);
        roleinfos = JSON.parseArray(json.getString("roleInfos"), RoleInfo.class);
        rolepowers = JSON.parseArray(json.getString("rolePowers"), RolePower.class);
        userroles = JSON.parseArray(json.getString("userRoles"), UserRole.class);
        unitinfos = JSON.parseArray(json.getString("unitInfos"), UnitInfo.class);
        userunits = JSON.parseArray(json.getString("userUnits"), UserUnit.class);
        datacatalogs = JSON.parseArray(json.getString("dataCatalogs"), DataCatalog.class);
        datadictionaies = JSON.parseArray(json.getString("dataDictionaries"), DataDictionary.class);
    }

    public static String loadJsonStringFormConfigFile(String fileName) throws IOException {
        String jsonFile = SysParametersUtils.getConfigHome()+ fileName;
        if(FileSystemOpt.existFile(jsonFile)) {
            return FileIOOpt.readStringFromFile(jsonFile,"UTF-8");
        }else{

            return FileIOOpt.readStringFromInputStream(
                    new ClassPathResource(fileName).getInputStream(),"UTF-8");

        }
    }
    /**
     * 刷新数据字典
     *
     * @return boolean 刷新数据字典
     */
    @Override
    public boolean reloadDictionary() {
        try {
            String jsonstr = loadJsonStringFormConfigFile("/static_system_config.json");
            loadConfigFromJSONString(jsonstr);
        } catch (IOException e) {
            userinfos = new ArrayList<>();
            optinfos = new ArrayList<>();
            optmethods = new ArrayList<>();
            roleinfos = new ArrayList<>();
            rolepowers = new ArrayList<>();
            userroles = new ArrayList<>();
            unitinfos = new ArrayList<>();
            userunits = new ArrayList<>();
            datacatalogs = new ArrayList<>();
            datadictionaies = new ArrayList<>();
            e.printStackTrace();
        }
        organizeDictionaryData();

        //static_system_user_pwd.json
        try {
            String jsonStr = loadJsonStringFormConfigFile("/static_system_user_pwd.json");
            JSONObject json = JSON.parseObject(jsonStr);
            for(UserInfo u :userinfos){
                String spwd = json.getString(u.getUserCode());
                if(StringUtils.isNotBlank(spwd))
                    u.setUserPin(spwd);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
        return true;
    }


    /**
     * 修改用户密码
     *
     * @param userCode userCode
     * @param userPassword userPassword
     */
    @Override
    public void changeUserPassword(String userCode, String userPassword) {
        UserInfo ui= getUserInfoByUserCode(userCode);
        if(ui==null)
            return;
        JSONObject json = null;
        String jsonFile = SysParametersUtils.getConfigHome()+"/static_system_user_pwd.json";
        try {
            String jsonstr = loadJsonStringFormConfigFile("/static_system_user_pwd.json");
            json = JSON.parseObject(jsonstr);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }

        if(json==null)
            json = new JSONObject();
        try {
            ui.setUserPin(passwordEncoder.createPassword(userPassword, userCode));
            json.put(userCode,ui.getUserPin());
            FileIOOpt.writeStringToFile(json.toJSONString(),jsonFile);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
    }
}
