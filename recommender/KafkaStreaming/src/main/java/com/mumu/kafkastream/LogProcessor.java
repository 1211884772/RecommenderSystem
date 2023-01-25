package com.mumu.kafkastream;
//
//                       .::::.
//                     .::::::::.
//                    :::::::::::
//                 ..:::::::::::'
//              '::::::::::::'
//                .::::::::::
//           '::::::::::::::..
//                ..::::::::::::.
//              ``::::::::::::::::
//               ::::``:::::::::'        .:::.
//              ::::'   ':::::'       .::::::::.
//            .::::'      ::::     .:::::::'::::.
//           .:::'       :::::  .:::::::::' ':::::.
//          .::'        :::::.:::::::::'      ':::::.
//         .::'         ::::::::::::::'         ``::::.
//     ...:::           ::::::::::::'              ``::.
//    ```` ':.          ':::::::::'                  ::::..
//                       '.:::::'                    ':'````..
//
//
//
//                  年少太轻狂，误入码农行。
//                  白发森森立，两眼直茫茫。
//                  语言数十种，无一称擅长。
//                  三十而立时，无房单身郎。
//
//

import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;

/**
 * @BelongsProject: GmallRecommendSystem
 * @BelongsPackage: com.mumu.kafkastream
 * @Description: TODO
 * @Author: mumu
 * @CreateTime: 2023-01-23  16:47
 * @Version: 1.0
 */
public class LogProcessor implements Processor<byte[],byte[]> {
    private ProcessorContext context;
    @Override
    public void init(ProcessorContext processorContext) {
        this.context=processorContext;
    }

    @Override
    public void process(byte[] dummy, byte[] line) {
        //核心处理流程
        String input = new String(line);
        //提取数据，以固定前缀过滤日志信息
        if(input.contains("PRODUCT_RATING_PREFIX:")){
            System.out.println("product rating data coming!"+input);
            input = input.split("PRODUCT_RATING_PREFIX:")[1].trim();
            context.forward("logProcessor".getBytes(),input.getBytes());
        }

    }

    @Override
    public void punctuate(long l) {

    }

    @Override
    public void close() {

    }
}
