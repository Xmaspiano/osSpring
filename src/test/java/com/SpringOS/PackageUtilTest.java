package com.SpringOS;

/**
 * Created by AlbertXmas on 16/8/29.
 */
import com.SpringOS.system.entity.ShiroResources;
import com.SpringOS.system.entity.common.IdEntity;
import com.SpringOS.system.service.ShiroResourcesService;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class PackageUtilTest {

    @Autowired
    private ShiroResourcesService shiroResourcesService;

    public static void main(String[] args) throws Exception{
        System.out.println("------------------");
//        if(event.getApplicationContext().getParent() == null) {
            List<ShiroResources> srList = new PackageUtilTest().getResourceList("com.SpringOS");
        System.out.println("--------"+srList+"----------");
            ShiroResources tempShiroResources = null;
            for (ShiroResources shiroResources: srList) {
//                tempShiroResources = shiroResourcesService.findOneBySome(shiroResources);
//                System.out.println("------------------\n"+tempShiroResources);
                System.out.println("------------------");
                if(tempShiroResources != null){
                    tempShiroResources.setShiroAuth(shiroResources.getShiroAuth());
                }else{
                    tempShiroResources = shiroResources;
                }

                System.out.println("------------------");
                Set<String> set = new PackageUtilTest().getAnalysis(shiroResources.getShiroAuth());

                for(String str:set){//默认只取三层
                    System.out.println(str);
                }
                System.out.println("------------------");

                if(!tempShiroResources.getShiroAuth().equals(shiroResources.getShiroAuth())) {
//                    shiroResourcesService.save(tempShiroResources);
                }
            }
//        }
    }


    /**
     * 从包package中获取所有的Class
     * @param
     * @return
     */
    public static List<Class<?>> getClasses(String packageName){

        //第一个class类的集合
        List<Class<?>> classes = new ArrayList<Class<?>>();
        //是否循环迭代
        boolean recursive = true;
        //获取包的名字 并进行替换
        String packageDirName = packageName.replace('.', '/');
        //定义一个枚举的集合 并进行循环来处理这个目录下的things
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            //循环迭代下去
            while (dirs.hasMoreElements()){
                //获取下一个元素
                URL url = dirs.nextElement();
                //得到协议的名称
                String protocol = url.getProtocol();
                //如果是以文件的形式保存在服务器上
                if ("file".equals(protocol)) {
                    //获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    //以文件的方式扫描整个包下的文件 并添加到集合中
                    findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
                } else if ("jar".equals(protocol)){
                    //如果是jar包文件
                    //定义一个JarFile
                    JarFile jar;
                    try {
                        //获取jar
                        jar = ((JarURLConnection) url.openConnection()).getJarFile();
                        //从此jar包 得到一个枚举类
                        Enumeration<JarEntry> entries = jar.entries();
                        //同样的进行循环迭代
                        while (entries.hasMoreElements()) {
                            //获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            //如果是以/开头的
                            if (name.charAt(0) == '/') {
                                //获取后面的字符串
                                name = name.substring(1);
                            }
                            //如果前半部分和定义的包名相同
                            if (name.startsWith(packageDirName)) {
                                int idx = name.lastIndexOf('/');
                                //如果以"/"结尾 是一个包
                                if (idx != -1) {
                                    //获取包名 把"/"替换成"."
                                    packageName = name.substring(0, idx).replace('/', '.');
                                }
                                //如果可以迭代下去 并且是一个包
                                if ((idx != -1) || recursive){
                                    //如果是一个.class文件 而且不是目录
                                    if (name.endsWith(".class") && !entry.isDirectory()) {
                                        //去掉后面的".class" 获取真正的类名
                                        String className = name.substring(packageName.length() + 1, name.length() - 6);
                                        try {
                                            //添加到classes
                                            classes.add(Class.forName(packageName + '.' + className));
                                        } catch (ClassNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return classes;
    }

    /**
     * 以文件的形式来获取包下的所有Class
     * @param packageName
     * @param packagePath
     * @param recursive
     * @param classes
     */
    public static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive, List<Class<?>> classes){
        //获取此包的目录 建立一个File
        File dir = new File(packagePath);
        //如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        //如果存在 就获取包下的所有文件 包括目录
        File[] dirfiles = dir.listFiles(new FileFilter() {
            //自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        //循环所有文件
        for (File file : dirfiles) {
            //如果是目录 则继续扫描
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "." + file.getName(),
                        file.getAbsolutePath(),
                        recursive,
                        classes);
            }
            else {
                //如果是java类文件 去掉后面的.class 只留下类名
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    //添加到集合中去
                    classes.add(Class.forName(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void init(){
        List authList = new ArrayList();

        List<Class<?>> classes = PackageUtilTest.getClasses("com.SpringOS");
        RequiresRoles rr;
        ShiroResources ss;
        for (Class clas :classes) {
            Method[] md = clas.getMethods();

            for(Method method: md){
                rr = method.getAnnotation(RequiresRoles.class);
                if(rr != null) {
                    for (String s : rr.value()) {
                        if (!s.equals("")) {
                            ss = new ShiroResources();
                            ss.setRealName(clas.getName());
                            ss.setMethod(method.getName());
                            ss.setShiroAuth(ss.getShiroAuth()+s);
                            authList.add(ss);
                            break;
                        }
                    }
                }

            }
        }
        System.out.println(authList);
    }

    class shiroSubject extends IdEntity {
        String realName = "";
        String method = "";
        String shiroAuth = "";


    }


    private List<ShiroResources> getResourceList(String packageName){
        List<ShiroResources> authList = new ArrayList<ShiroResources>();

        List<Class<?>> classes = this.getClasses(packageName);
        RequiresRoles rr;
        ShiroResources ss;
        for (Class clas : classes) {
            Method[] md = clas.getMethods();

            for (Method method : md) {
                rr = method.getAnnotation(RequiresRoles.class);
                if (rr != null) {
                    for (String s : rr.value()) {
                        if (!s.equals("")) {
                            ss = new ShiroResources();
                            ss.setRealName(clas.getName());
                            ss.setMethod(method.getName());
                            ss.setShiroAuth(ss.getShiroAuth() + s);
                            authList.add(ss);
                            break;
                        }
                    }
                }

            }
        }
        return authList;
    }

    @Test
    public void onApplicationEvent(ContextRefreshedEvent event) {
//        System.out.println("------------------");
        if(event.getApplicationContext().getParent() == null) {
            List<ShiroResources> srList = getResourceList("com.SpringOS");

            ShiroResources tempShiroResources;
            for (ShiroResources shiroResources: srList) {
                tempShiroResources = shiroResourcesService.clearRepetitionData(shiroResources);
                System.out.println("------------------\n"+tempShiroResources);
                if(tempShiroResources != null){
                    tempShiroResources.setShiroAuth(shiroResources.getShiroAuth());
                }else{
                    tempShiroResources = shiroResources;
                }

                System.out.println("------------------");
                Set<String> set = getAnalysis(shiroResources.getShiroAuth());

                for(String str:set){//默认只取三层
                    System.out.println(str);
                }
                System.out.println("------------------");

                if(!tempShiroResources.getShiroAuth().equals(shiroResources.getShiroAuth())) {
                    shiroResourcesService.save(tempShiroResources);
                }
            }
        }
    }

    private Set<String> getAnalysis(String shiroResources){
        Set<String> set = new HashSet<String>();
        String[] main = shiroResources.split(",");
        if(main.length > 1 && main[0].indexOf(":") > 0) {
            String[] masterTag = {
                    main[0].substring(0, main[0].lastIndexOf(":")),
                    main[0].substring(main[0].lastIndexOf(":"))
            };
            main[0] = masterTag[1];

            for (String str : main) {
                set.add(masterTag[0] + str);
            }
        }else{
            set.add(shiroResources);
        }
        return set;
    }
}
