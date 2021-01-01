package org.noear.nami;

import org.noear.nami.annotation.Mapping;
import org.noear.nami.annotation.NamiClient;
import org.noear.solon.core.util.PathAnalyzer;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fairy - 调用处理程序
 *
 * @author noear
 * @since 1.0
 * */
public class NamiHandler implements InvocationHandler {
    private static Pattern pathKeyExpr = Pattern.compile("\\{([^\\\\}]+)\\}");

    private final NamiConfig config;

    private final Map<String, String> headers0 = new LinkedHashMap<>();
    private final String name0; //upstream name
    private final String path0; //path
    private final String url0;  //url
    private final Class<?> clz0;
    private final Map<String,Map> pathKeysCached = new ConcurrentHashMap<>();

    /**
     * @param config 配置
     * @param client 客户端注解
     */
    public NamiHandler(Class<?> clz, NamiConfig config, NamiClient client) {
        this.config = config;

        this.clz0 = clz;

        //1.运行配置器
        if (client != null) {
            try {
                NamiConfiguration tmp = client.configuration().newInstance();

                if (tmp != null) {
                    tmp.config(client, new Nami.Builder(config));
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            //>>添加接口header
            if (client.headers() != null) {
                for (String h : client.headers()) {
                    String[] ss = h.split("=");
                    if (ss.length == 2) {
                        headers0.put(ss[0].trim(), ss[1].trim());
                    }
                }
            }
        }

        //2.配置初始化
        config.tryInit();

        //3.获取 or url
        String uri = config.getUri();

        if (uri == null && client != null) {
            //1.优先从 XClient 获取服务地址或名称
            if (isEmpty(client.value()) == false) {
                uri = client.value();
            }
        }

        //2.如果没有，就报错
        if (uri == null) {
            throw new NamiException("NamiClient config is wrong: " + clz.getName());
        }

        if (uri.contains("://")) {
            url0 = uri;
            name0 = null;
            path0 = null;
        } else {
            if (uri.contains(":")) {
                url0 = null;
                name0 = uri.split(":")[0];
                path0 = uri.split(":")[1];
            } else {
                url0 = null;
                name0 = uri;
                path0 = null;
            }
        }
    }


    protected MethodHandles.Lookup lookup;

    @Override
    public Object invoke(Object proxy, Method method, Object[] vals) throws Throwable {
        if (url0 == null && config.getUpstream() == null) {
            throw new NamiException("NamiClient: Not found upstream: " + clz0.getName());
        }

        Class caller = method.getDeclaringClass();
        if (Object.class == caller) {
            if (this.lookup == null) {
                Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Integer.TYPE);
                constructor.setAccessible(true);
                this.lookup = constructor.newInstance(caller, MethodHandles.Lookup.PRIVATE);
            }

            return this.lookup.unreflectSpecial(method, caller).bindTo(proxy).invokeWithArguments(vals);
        }

        //构建 headers
        Map<String, String> headers = new HashMap<>(headers0);

        //构建 args
        Map<String, Object> args = new LinkedHashMap<>();
        Parameter[] names = method.getParameters();
        for (int i = 0, len = names.length; i < len; i++) {
            if (vals[i] != null) {
                args.put(names[i].getName(), vals[i]);
            }
        }

        //构建 fun
        String fun = method.getName();
        String act = null;
        String mappingVal = null;
        Mapping mapping = method.getAnnotation(Mapping.class);
        if (mapping != null && isEmpty(mapping.value()) == false) {
            //格式1: GET
            //格式2: GET user/a.0.1
            mappingVal = mapping.value().trim();

            if (mappingVal.indexOf(" ") > 0) {
                act = mappingVal.split(" ")[0];
                fun = mappingVal.split(" ")[1];
            }else{
                act = mappingVal;
            }

            if (mapping.headers() != null) {
                for (String h : mapping.headers()) {
                    String[] ss = h.split("=");
                    if (ss.length == 2) {
                        headers.put(ss[0].trim(), ss[1].trim());
                    }
                }
            }
        }





        //构建 url
        String url = null;
        if (url0 == null) {
            url = config.getUpstream().get();

            if (url == null) {
                throw new NamiException("NamiClient: Not found upstream!");
            }

            if (path0 != null) {
                int idx = url.indexOf("/", 9);//https://a
                if (idx > 0) {
                    url = url.substring(0, idx);
                }

                if (path0.endsWith("/")) {
                    fun = path0 + fun;
                } else {
                    fun = path0 + "/" + fun;
                }
            }

        } else {
            url = url0;
        }

        if(mappingVal !=null && mappingVal.indexOf("{") > 0) {
            //
            //处理Path参数
            //
            Map<String, String> pathKeys = buildPathKeys(mappingVal);

            for (Map.Entry<String, String> kv : pathKeys.entrySet()) {
                String val = (String) args.get(kv.getValue());

                if (val != null) {
                    url.replace(kv.getKey(), val);
                    args.remove(kv.getValue());
                }
            }
        }


        //执行调用
        return new Nami(config)
                .method(method)
                .action(act)
                .url(url, fun)
                .call(headers, args)
                .getObject(method.getReturnType());
    }


    private static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    private  Map<String,String> buildPathKeys(String path){
        Map<String,String> pathKeys = pathKeysCached.get(path);
        if(pathKeys == null){
            synchronized (path.intern()){
                pathKeys = pathKeysCached.get(path);
                if(pathKeys == null){
                    pathKeys = new LinkedHashMap<>();

                    Matcher pm = pathKeyExpr.matcher(path);

                    while (pm.find()) {
                        pathKeys.put(pm.group(), pm.group(1));
                    }
                }
            }
        }

        return pathKeys;
    }
}
