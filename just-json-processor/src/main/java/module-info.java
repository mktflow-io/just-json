module io.mktflow.json.processor {
    requires io.mktflow.json;
    requires java.compiler;

    provides javax.annotation.processing.Processor
            with io.mktflow.json.processor.JsonRecordProcessor;
}
