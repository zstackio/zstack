package org.zstack.core.errorcode;

/**
 * Created by mingjian.deng on 2018/12/21.
 */
public enum ElaborationFailedReason {
    FileNameWithoutJson("file name must endWith '.json'"),
    FileNameAlreadyExisted("elaborate file name already existed in zstack"),
    DuplicatedFileName("input filename duplicated"),
    RegexAlreadyExisted("regex already existed in another json"),
    DuplicatedRegex("regex duplicate in at least 2 input files"),
    NotSameCategoriesInFile("not all categories are same in 1 input file"),
    MessageNotFound("can not found message_cn for the regex"),
    RegexNotFound("can not found regex for the segment"),
    InValidJsonSchema("invalid json schema"),
    OtherReason("OtherReason");

    String type;

    ElaborationFailedReason(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public static ElaborationFailedReason get(String type) {
        for (ElaborationFailedReason tmp: ElaborationFailedReason.values()) {
            if (tmp.toString().equalsIgnoreCase(type)) {
                return tmp;
            }
        }
        return ElaborationFailedReason.valueOf(type);
    }
}
