package org.zstack.abstraction

import java.lang.Boolean
import java.lang.Integer
import java.lang.Long
import org.zstack.abstraction.OptionType.InputType

doc {

	title "插件动态配置字段"

	field {
		name "uuid"
		desc "配置字段UUID"
		type "String"
		since "5.3.28"
	}
	field {
		name "name"
		desc "配置字段名称"
		type "String"
		since "5.3.28"
	}
	field {
		name "code"
		desc "配置字段唯一编码"
		type "String"
		since "5.3.28"
	}
	field {
		name "category"
		desc "配置字段分类"
		type "String"
		since "5.3.28"
	}
	field {
		name "required"
		desc "是否必填"
		type "Boolean"
		since "5.3.28"
	}
	field {
		name "editable"
		desc "是否允许编辑"
		type "Boolean"
		since "5.3.28"
	}
	field {
		name "enabled"
		desc "是否启用"
		type "Boolean"
		since "5.3.28"
	}
	field {
		name "displayOrder"
		desc "字段显示顺序"
		type "Integer"
		since "5.3.28"
	}
	ref {
		name "inputType"
		path "org.zstack.abstraction.OptionType.inputType"
		desc "字段输入类型"
		type "InputType"
		since "5.3.28"
		clz InputType.class
	}
	field {
		name "placeHolderText"
		desc "输入框占位提示"
		type "String"
		since "5.3.28"
	}
	field {
		name "defaultValue"
		desc "字段默认值"
		type "String"
		since "5.3.28"
	}
	field {
		name "noSelection"
		desc "未选择选项时使用的值"
		type "String"
		since "5.3.28"
	}
	field {
		name "noBlank"
		desc "是否禁止空白值"
		type "Boolean"
		since "5.3.28"
	}
	field {
		name "secretField"
		desc "是否为敏感字段"
		type "Boolean"
		since "5.3.28"
	}
	field {
		name "minVal"
		desc "允许的最小数值"
		type "Long"
		since "5.3.28"
	}
	field {
		name "maxVal"
		desc "允许的最大数值"
		type "Long"
		since "5.3.28"
	}
	field {
		name "minLength"
		desc "允许的最小输入长度"
		type "Long"
		since "5.3.28"
	}
	field {
		name "maxLength"
		desc "允许的最大输入长度"
		type "Long"
		since "5.3.28"
	}
	field {
		name "fieldContext"
		desc "字段所属配置上下文"
		type "String"
		since "5.3.28"
	}
	field {
		name "fieldClass"
		desc "字段展示样式类"
		type "String"
		since "5.3.28"
	}
	field {
		name "fieldLabel"
		desc "字段展示名称"
		type "String"
		since "5.3.28"
	}
	field {
		name "fieldCode"
		desc "字段国际化编码"
		type "String"
		since "5.3.28"
	}
	field {
		name "fieldName"
		desc "字段保存键名"
		type "String"
		since "5.3.28"
	}
	field {
		name "fieldGetName"
		desc "读取字段值时使用的键名"
		type "String"
		since "5.3.28"
	}
	field {
		name "fieldSetName"
		desc "保存字段值时使用的键名"
		type "String"
		since "5.3.28"
	}
	field {
		name "fieldGetContext"
		desc "读取字段值时使用的配置上下文"
		type "String"
		since "5.3.28"
	}
	field {
		name "fieldSetContext"
		desc "保存字段值时使用的配置上下文"
		type "String"
		since "5.3.28"
	}
	field {
		name "fieldGroup"
		desc "字段所属展示分组"
		type "String"
		since "5.3.28"
	}
	field {
		name "fieldGroupI18nCode"
		desc "字段分组国际化编码"
		type "String"
		since "5.3.28"
	}
	field {
		name "helpText"
		desc "字段帮助信息"
		type "String"
		since "5.3.28"
	}
	field {
		name "helpTextI18nCode"
		desc "字段帮助信息国际化编码"
		type "String"
		since "5.3.28"
	}
	field {
		name "optionSourceType"
		desc "动态选项的数据源类型"
		type "String"
		since "5.3.28"
	}
	field {
		name "optionSource"
		desc "动态选项的数据源"
		type "String"
		since "5.3.28"
	}
	field {
		name "dependsOn"
		desc "当前字段依赖的其他字段"
		type "String"
		since "5.3.28"
	}
	field {
		name "showOnEdit"
		desc "编辑时是否显示"
		type "Boolean"
		since "5.3.28"
	}
	field {
		name "displayValueOnDetails"
		desc "详情页是否显示字段值"
		type "Boolean"
		since "5.3.28"
	}
	field {
		name "showOnCreate"
		desc "创建时是否显示"
		type "Boolean"
		since "5.3.28"
	}
	field {
		name "verifyPattern"
		desc "字段值校验正则表达式"
		type "String"
		since "5.3.28"
	}
}
