
package com.marketo.mktows;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour ListKeyType.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="ListKeyType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="MKTOLISTNAME"/>
 *     &lt;enumeration value="MKTOSALESUSERID"/>
 *     &lt;enumeration value="SFDCLEADOWNERID"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ListKeyType")
@XmlEnum
public enum ListKeyType {

    MKTOLISTNAME,
    MKTOSALESUSERID,
    SFDCLEADOWNERID;

    public String value() {
        return name();
    }

    public static ListKeyType fromValue(String v) {
        return valueOf(v);
    }

}
