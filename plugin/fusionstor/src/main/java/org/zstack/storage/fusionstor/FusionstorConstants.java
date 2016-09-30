package org.zstack.storage.fusionstor;
/**
 * Created by frank on 7/27/2015.
 */

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface FusionstorConstants {
    @PythonClass
    String FUSIONSTOR_BACKUP_STORAGE_TYPE = "Fusionstor";
    @PythonClass
    String FUSIONSTOR_PRIMARY_STORAGE_TYPE = "Fusionstor";

    String MON_PARAM_MON_PORT = "monPort";
}
