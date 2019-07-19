package org.zstack.core.errorcode;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.*;
import org.zstack.header.message.Message;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.string.ErrorCodeElaboration;
import org.zstack.utils.string.StringSimilarity;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/12/1.
 */
public class ElaborationManagerImpl extends AbstractService {
    static final CLogger logger = Utils.getLogger(ElaborationManagerImpl.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIReloadElaborationMsg) {
            handle((APIReloadElaborationMsg) msg);
        } else if (msg instanceof APIGetElaborationsMsg) {
            handle((APIGetElaborationsMsg) msg);
        } else if (msg instanceof APIGetElaborationCategoriesMsg) {
            handle((APIGetElaborationCategoriesMsg) msg);
        } else if (msg instanceof APIGetMissedElaborationMsg) {
            handle((APIGetMissedElaborationMsg) msg);
        } else if (msg instanceof APICheckElaborationContentMsg) {
            handle((APICheckElaborationContentMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void preCheckElaborationContent(String filename, String jsonContent, ReturnValueCompletion<List<ElaborationCheckResult>> completion) {
        if (filename == null && jsonContent == null) {
            completion.fail(argerr("non file or jsoncontent input"));
            return;
        }

        if (filename != null && jsonContent != null) {
            completion.fail(argerr("file or jsoncontent cannot both nonempty"));
            return;
        }

        final boolean isClassPathFolder = (StringSimilarity.classPathFolder != null && StringSimilarity.classPathFolder.getAbsolutePath().equalsIgnoreCase(filename));

        List<String> errorTemplates = PathUtil.scanFolderOnClassPath(StringSimilarity.elaborateFolder);
        List<String> errTemplates = new ArrayList<>();
        errorTemplates.forEach(e -> errTemplates.add(PathUtil.fileName(e)));

        FlowChain checks = FlowChainBuilder.newShareFlowChain();
        checks.setName(String.format("check-elaborations-for-%s", filename));

        checks.then(new ShareFlow() {
            List<String> files = new ArrayList<>();
            Map<String, List<ErrorCodeElaboration>> contents = new HashMap<>();
            List<ElaborationCheckResult> results = new ArrayList<>();

            @Override
            public void setup() {
                if (filename != null) {
                    flow(new NoRollbackFlow() {
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            try {
                                File folder = new File(filename);
                                if (folder.isFile()) {
                                    files.add(folder.getAbsolutePath());
                                } else {
                                    PathUtil.scanFolder(files, folder.getAbsolutePath());
                                }
                            } catch (Exception e) {
                                trigger.fail(operr("Unable to scan folder: %s", e.getMessage()));
                                return;
                            }
                            if (files.isEmpty()) {
                                trigger.fail(argerr("%s is not existed or is empty folder", filename));
                            } else {
                                trigger.next();
                            }
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "FileNameWithoutJson";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            for (String file: files) {
                                String name = PathUtil.fileName(file);
                                if (!name.endsWith(".json")) {
                                    results.add(new ElaborationCheckResult(file, null, ElaborationFailedReason.FileNameWithoutJson.toString()));
                                }
                            }
                            trigger.next();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "FileNameAlreadyExisted and DuplicatedFileName";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            HashSet<String> sets = new HashSet<>();
                            for (String file: files) {
                                String name = PathUtil.fileName(file);
                                if (!isClassPathFolder && errTemplates.contains(name)) {
                                    results.add(new ElaborationCheckResult(file, null, ElaborationFailedReason.FileNameAlreadyExisted.toString()));
                                }
                                if (sets.contains(name)) {
                                    results.add(new ElaborationCheckResult(file, null, ElaborationFailedReason.DuplicatedFileName.toString()));
                                } else {
                                    sets.add(name);
                                }
                            }
                            trigger.next();
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "InValidJsonArraySchema";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            List<String> checks = CollectionDSL.list();
                            checks.addAll(files);
                            for (String file: checks) {
                                File templateFile = new File(file);
                                try {
                                    String content = FileUtils.readFileToString(templateFile);
                                    new JsonParser().parse(content);
                                    List<ErrorCodeElaboration> errs = JSONObjectUtil.toCollection(content, ArrayList.class, ErrorCodeElaboration.class);
                                    contents.put(file, errs);
                                } catch (IOException e) {
                                    trigger.fail(Platform.operr(String.format("read error elaboration template files [%s] failed, due to: %s", templateFile, e.getMessage())));
                                    return;
                                } catch (JsonSyntaxException e) {
                                    results.add(new ElaborationCheckResult(file, null, ElaborationFailedReason.InValidJsonSchema.toString()));
                                    files.remove(file);
                                } catch (JSONException e) {
                                    results.add(new ElaborationCheckResult(file, null, ElaborationFailedReason.InValidJsonArraySchema.toString()));
                                    files.remove(file);
                                } catch (Exception e) {
                                    logger.debug(e.getMessage());
                                    results.add(new ElaborationCheckResult(file, null, ElaborationFailedReason.InValidJsonArraySchema.toString()));
                                    files.remove(file);
                                }
                            }
                            trigger.next();
                        }
                    });
                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = "InValidJsonArraySchema";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            try {
                                String filename = "input-json";
                                new JsonParser().parse(jsonContent);
                                List<ErrorCodeElaboration> errs = JSONObjectUtil.toCollection(jsonContent, ArrayList.class, ErrorCodeElaboration.class);
                                contents.put(filename, errs);
                            } catch (JsonSyntaxException e) {
                                results.add(new ElaborationCheckResult(filename, null, ElaborationFailedReason.InValidJsonSchema.toString()));
                            } catch (JSONException e) {
                                results.add(new ElaborationCheckResult(filename, null, ElaborationFailedReason.InValidJsonArraySchema.toString()));
                            } catch (Exception e) {
                                logger.debug(e.getMessage());
                                results.add(new ElaborationCheckResult(filename, null, ElaborationFailedReason.InValidJsonArraySchema.toString()));
                            }
                            trigger.next();
                        }
                    });
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "RegexAlreadyExisted, DuplicatedRegex, MessageNotFound and RegexNotFound";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        HashSet<String> sets = new HashSet<>();
                        contents.forEach((f, c) -> {
                            for (ErrorCodeElaboration err: c) {
                                if (err.getRegex() == null || err.getRegex().isEmpty()) {
                                    results.add(new ElaborationCheckResult(f, err.getCategory() + "." + err.getCode(), ElaborationFailedReason.RegexNotFound.toString()));
                                    continue;
                                }

                                if (err.getMessage_cn() == null || err.getMessage_cn().isEmpty()) {
                                    results.add(new ElaborationCheckResult(f, err.getCategory() + "." + err.getCode(), ElaborationFailedReason.MessageNotFound.toString()));
                                }

                                if (!isClassPathFolder && StringSimilarity.regexContained(err.getRegex())) {
                                    results.add(new ElaborationCheckResult(f, err.getCategory() + "." + err.getCode(), ElaborationFailedReason.RegexAlreadyExisted.toString()));
                                }

                                if (sets.contains(err.getRegex())) {
                                    results.add(new ElaborationCheckResult(f, err.getCategory() + "." + err.getCode(), ElaborationFailedReason.DuplicatedRegex.toString()));
                                } else {
                                    sets.add(err.getRegex());
                                }
                            }

                        });
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "CategoryNotFound and NotSameCategoriesInFile";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        Map<String, String> categories = new HashMap<>();
                        contents.forEach((f, c) -> {
                            for (ErrorCodeElaboration err: c) {
                                if (err.getCategory() == null || err.getCategory().isEmpty()) {
                                    results.add(new ElaborationCheckResult(f, err.getCategory() + "." + err.getCode(), ElaborationFailedReason.CategoryNotFound.toString()));
                                    continue;
                                }

                                if (categories.get(f) == null) {
                                    categories.put(f, err.getCategory());
                                } else {
                                    if (!categories.get(f).equals(err.getCategory())) {
                                        results.add(new ElaborationCheckResult(f, err.getCategory() + "." + err.getCode(), ElaborationFailedReason.NotSameCategoriesInFile.toString()));
                                    }
                                }
                            }
                        });
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "DuplicatedErrorCode and ErrorCodeAlreadyExisted";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        HashSet<String> sets = new HashSet<>();
                        contents.forEach((f, c) -> {
                            for (ErrorCodeElaboration err: c) {
                                if (err.getCode() == null || err.getCode().isEmpty() || err.getCategory() == null || err.getCategory().isEmpty()) {
                                    continue;
                                }

                                if (!NumberUtils.isNumber(err.getCode())) {
                                    trigger.fail(operr("elaboration code must be number!"));
                                    return;
                                }
                                String code = err.getCategory() + "." + err.getCode();

                                if (!isClassPathFolder && StringSimilarity.errorCodeContained(code)) {
                                    results.add(new ElaborationCheckResult(f, code, ElaborationFailedReason.ErrorCodeAlreadyExisted.toString()));
                                }

                                if (sets.contains(code)) {
                                    results.add(new ElaborationCheckResult(f, code, ElaborationFailedReason.DuplicatedErrorCode.toString()));
                                } else {
                                    sets.add(code);
                                }
                            }

                        });
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success(results);
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(final APICheckElaborationContentMsg msg) {
        APICheckElaborationContentReply reply = new APICheckElaborationContentReply();
        preCheckElaborationContent(msg.getElaborateFile(), msg.getElaborateContent(), new ReturnValueCompletion<List<ElaborationCheckResult>>(msg) {
            @Override
            public void success(List<ElaborationCheckResult> returnValue) {
                returnValue.sort(Comparator.comparing(ElaborationCheckResult::getReason));
                reply.setResults(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private List<ElaborationVO> getMissedElatorations(Long repeats, String from) {
        Long times = repeats != null ? repeats : 1L;
        if (from == null) {
            return Q.New(ElaborationVO.class).eq(ElaborationVO_.matched, false).gte(ElaborationVO_.repeats, times).list();
        } else {
            if (TimeUtils.isValidTimestampFormat(from)) {
                long start = TimeUtils.parseFormatStringToTimeStamp(from);
                return Q.New(ElaborationVO.class).eq(ElaborationVO_.matched, false).gte(ElaborationVO_.repeats, times).
                        gte(ElaborationVO_.lastOpDate, new Timestamp(start)).list();
            } else if (NumberUtils.isNumber(from)) {
                try {
                    return Q.New(ElaborationVO.class).eq(ElaborationVO_.matched, false).gte(ElaborationVO_.repeats, times).
                            gte(ElaborationVO_.lastOpDate, new Timestamp(Long.valueOf(from))).list();
                } catch (NumberFormatException e) {
                    throw new OperationFailureException(argerr("%s is not a Long value Number", from));
                }
            } else {
                throw new OperationFailureException(argerr("arg 'startTime' should format like 'yyyy-MM-dd HH:mm:ss' or '1545380003000'"));
            }
        }
    }

    private void handle(final APIGetMissedElaborationMsg msg) {
        APIGetMissedElaborationReply reply = new APIGetMissedElaborationReply();
        List<ElaborationVO> vos = getMissedElatorations(msg.getRepeats(), msg.getStartTime());

        vos.forEach(vo -> {
            ErrorCodeElaboration e = StringSimilarity.findSimilary(vo.getErrorInfo());
            if (!StringSimilarity.matched(e)) {
                reply.getInventories().add(ElaborationInventory.valueOf(vo));
            }
        });
        bus.reply(msg, reply);
    }

    private void eliminateErrors() {
        String time = ElaborateGlobalConfig.ELIMILATE_TIME.value();
        long count = Q.New(ElaborationVO.class).eq(ElaborationVO_.matched, false).lt(ElaborationVO_.lastOpDate, new Timestamp(new Date().getTime() - TimeUtils.parseTimeInMillis(time))).count();
        if (count > 0) {
            logger.debug(String.format("clean [%s] records which are not matched error code in db", count));
            UpdateQuery.New(ElaborationVO.class).eq(ElaborationVO_.matched, false).lt(ElaborationVO_.lastOpDate, new Timestamp(new Date().getTime() - TimeUtils.parseTimeInMillis(time))).delete();
        }
    }

    private void refreshElaboration(final Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("refresh-elaboration");
        chain.then(new NoRollbackFlow() {
            String __name__ = "check elaboration contents first";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                preCheckElaborationContent(StringSimilarity.classPathFolder.getAbsolutePath(), null, new ReturnValueCompletion<List<ElaborationCheckResult>>(trigger) {
                    @Override
                    public void success(List<ElaborationCheckResult> returnValue) {
                        if (returnValue.isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(operr("%s", returnValue.get(0).getReason()));
                        }
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "refresh error templates";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                StringSimilarity.refreshErrorTemplates();
                eliminateErrors();
                List<ElaborationVO> vos = Q.New(ElaborationVO.class).gte(ElaborationVO_.repeats, 1).eq(ElaborationVO_.matched, false).orderBy(ElaborationVO_.lastOpDate, SimpleQuery.Od.DESC).list();
                if (!vos.isEmpty()) {
                    List<String> messages = StringSimilarity.getElaborations().stream().map(ErrorCodeElaboration::getMessage_cn).collect(Collectors.toList());
                    for (ElaborationVO vo: vos) {
                        if (messages.contains(vo.getErrorInfo())) {
                            vo.setMatched(true);
                            dbf.updateAndRefresh(vo);
                        }
                    }
                }
                StringSimilarity.resetCachedErrors();
                trigger.next();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(final APIReloadElaborationMsg msg) {
        APIReloadElaborationEvent evt = new APIReloadElaborationEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                refreshElaboration(new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return "reload-elaborations";
            }
        });
    }

    private void handle(final APIGetElaborationsMsg msg) {
        APIGetElaborationsReply reply = new APIGetElaborationsReply();

        if (msg.getRegex() != null) {
            ErrorCodeElaboration e = StringSimilarity.findSimilary(msg.getRegex());
            if (StringSimilarity.matched(e)) {
                if (msg.getCategory() != null && msg.getCategory().equals(e.getCategory())) {
                    reply.getContents().add(new ElaborationContent(e));
                } else if (msg.getCategory() == null){
                    reply.getContents().add(new ElaborationContent(e));
                }
            }
        } else if (msg.getCategory() != null) {
            List<ErrorCodeElaboration> elaborations = StringSimilarity.getElaborations();
            elaborations.forEach(e -> {
                if (e.getCategory().equals(msg.getCategory())) {
                    reply.getContents().add(new ElaborationContent(e));
                }
            });
        }


        if (msg.getCategory() == null && msg.getRegex() == null){
            throw new OperationFailureException(Platform.argerr("input args 'regex' or 'category' must be set"));
        }

        bus.reply(msg, reply);
    }

    private void handle(final APIGetElaborationCategoriesMsg msg) {
        APIGetElaborationCategoriesReply reply = new APIGetElaborationCategoriesReply();
        List<ErrorCodeElaboration> elaborations = StringSimilarity.getElaborations();
        Map<String, ElaborationCategory> categoryMap = new HashMap<>();
        for (ErrorCodeElaboration elaboration: elaborations) {
            ElaborationCategory c = categoryMap.get(elaboration.getCategory());
            if (c != null) {
                c.setNum(c.getNum() + 1);
                categoryMap.put(elaboration.getCategory(), c);
            } else {
                categoryMap.put(elaboration.getCategory(), new ElaborationCategory(elaboration.getCategory(), 1));
            }
        }

        if (!categoryMap.isEmpty()) {
            categoryMap.forEach((key, value) -> reply.getCategories().add(value));
        }

        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ElaborationConsstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        installGlobalConfigValidator();
        return true;
    }

    private void installGlobalConfigValidator() {
        ElaborateGlobalConfig.ELIMILATE_TIME.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                if (!TimeUtils.isValidTimeFormat(newValue)) {
                    throw new GlobalConfigException(String.format("%s is not a valid format string;" +
                            " a format string consists of a number ending with suffix s/S/m/M/h/H/d/D/w/W/y/Y or without suffix;" +
                            " for example, 3h, 1y", newValue));
                }
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }
}
