package org.zstack.compute.zone;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.BeforeZoneCascadeDeleteExtensionPoint;
import org.zstack.core.componentloader.PluginExtension;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.zone.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

class ZoneExtensionPointEmitter implements Component {
	private static final CLogger logger = Utils.getLogger(ZoneExtensionPointEmitter.class);

	@Autowired
	private PluginRegistry pluginRgty;
    @Autowired
    private ErrorFacade errf;

    private List<ZoneDeleteExtensionPoint> delExts;
    private List<BeforeZoneCascadeDeleteExtensionPoint> cascadeDelExts;
    private List<ZoneChangeStateExtensionPoint> changeExts;

	void preDelete(ZoneInventory zinv) throws ZoneException {
		for (ZoneDeleteExtensionPoint extp : delExts) {
			try {
				extp.preDeleteZone(zinv);
            } catch (ZoneException ze) {
                logger.debug(String.format("extension[%s] refused to delete zone[name: %s, uuid:%s] because %s", extp.getClass().getName(), zinv.getName(), zinv.getUuid(), ze.getMessage()));
                throw ze;
			} catch (Exception e) {
				logger.warn("Exception happened while calling " + extp.getClass().getCanonicalName() + ".preDelete, " + "zone name: " + zinv.getName()
				        + " uuid: " + zinv.getUuid(), e);
			}
		}
	}
	
	void beforeDelete(final ZoneInventory zinv) {
        CollectionUtils.safeForEach(delExts, new ForEachFunction<ZoneDeleteExtensionPoint>() {
            @Override
            public void run(ZoneDeleteExtensionPoint arg) {
                arg.beforeDeleteZone(zinv);
            }
        });
	}
	
	void afterDelete(final ZoneInventory zinv) {
        CollectionUtils.safeForEach(delExts, new ForEachFunction<ZoneDeleteExtensionPoint>() {
            @Override
            public void run(ZoneDeleteExtensionPoint arg) {
                arg.afterDeleteZone(zinv);
            }
        });
	}

    void prepareCascadeDelete(ZoneInventory inventory, CascadeAction action, Completion completion) {
        List<BeforeZoneCascadeDeleteExtensionPoint> prepared = new ArrayList<>();
        new While<>(cascadeDelExts).each((extension, whileCompletion) -> {
            try {
                extension.beforeDelete(inventory, action, new Completion(whileCompletion) {
                    @Override
                    public void success() {
                        prepared.add(extension);
                        whileCompletion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        whileCompletion.addError(errorCode);
                        whileCompletion.allDone();
                    }
                });
            } catch (RuntimeException e) {
                whileCompletion.addError(errf.throwableToInternalError(e));
                whileCompletion.allDone();
            }
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errors) {
                if (errors.getCauses().isEmpty()) {
                    completion.success();
                    return;
                }
                cancelCascadeDelete(inventory, action, prepared, errors.getCauses().get(0), completion);
            }
        });
    }

    void cancelCascadeDelete(ZoneInventory inventory, CascadeAction action, Completion completion) {
        cancelCascadeDelete(inventory, action, cascadeDelExts, null, completion);
    }

    private void cancelCascadeDelete(ZoneInventory inventory, CascadeAction action,
                                     List<BeforeZoneCascadeDeleteExtensionPoint> extensions,
                                     ErrorCode originalError,
                                     Completion completion) {
        new While<>(extensions).each((extension, whileCompletion) -> {
            try {
                extension.cancel(inventory, action, new Completion(whileCompletion) {
                    @Override
                    public void success() {
                        whileCompletion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        whileCompletion.addError(errorCode);
                        whileCompletion.allDone();
                    }
                });
            } catch (RuntimeException e) {
                whileCompletion.addError(errf.throwableToInternalError(e));
                whileCompletion.allDone();
            }
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errors) {
                if (originalError != null) {
                    completion.fail(originalError);
                } else if (errors.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errors.getCauses().get(0));
                }
            }
        });
    }
	
	void preChange(ZoneVO vo, ZoneStateEvent event) throws ZoneException {
		ZoneInventory zinv = ZoneInventory.valueOf(vo);
		ZoneState next = AbstractZone.getNextState(vo.getState(), event);
		for (ZoneChangeStateExtensionPoint extp : changeExts) {
			try {
				extp.preChangeZoneState(zinv, event, next);
            } catch (ZoneException ze) {
                logger.debug(String.format("Extension: %s refused zone change state operation[ZoneStateEvent:%s] because %s", extp.getClass()
                        .getCanonicalName(), event, ze.getMessage()));
                throw ze;
			} catch (Exception e) {
				logger.warn("Exception happened while calling " + extp.getClass().getCanonicalName() + ".preChangeZoneState(), " + "zone name: " + zinv.getName()
				        + " uuid: " + zinv.getUuid(), e);
			}
		}
	}
	
	void beforeChange(ZoneVO vo, final ZoneStateEvent event) {
		final ZoneInventory zinv = ZoneInventory.valueOf(vo);
		final ZoneState next = AbstractZone.getNextState(vo.getState(), event);
        CollectionUtils.safeForEach(changeExts, new ForEachFunction<ZoneChangeStateExtensionPoint>() {
            @Override
            public void run(ZoneChangeStateExtensionPoint arg) {
                arg.beforeChangeZoneState(zinv, event, next);
            }
        });
	}
	
	void afterChange(ZoneVO vo, final ZoneStateEvent event, final ZoneState previousState) {
		final ZoneInventory zinv = ZoneInventory.valueOf(vo);
        CollectionUtils.safeForEach(changeExts, new ForEachFunction<ZoneChangeStateExtensionPoint>() {
            @Override
            public void run(ZoneChangeStateExtensionPoint arg) {
                arg.afterChangeZoneState(zinv, event, previousState);
            }
        });
	}

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    private void populateExtensions() {
        delExts = pluginRgty.getExtensionList(ZoneDeleteExtensionPoint.class);
        cascadeDelExts = pluginRgty.getExtensionList(BeforeZoneCascadeDeleteExtensionPoint.class);
        changeExts = pluginRgty.getExtensionList(ZoneChangeStateExtensionPoint.class);
    }

    @Override
    public boolean stop() {
        return true;
    }
}
