package org.zstack.test.core.db;

import org.zstack.header.vo.Uuid;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "person")
public class PersonVO {
    public static enum Sex {
        MALE,
        FEMALE,
    }

    /**
     * @param id
     * @param name
     * @param uuid
     * @param description
     * @param age
     * @param sex
     * @param marriage
     * @param title
     * @param date
     */
    public PersonVO(String name, String description, int age, Sex sex, boolean marriage, String title, Date date) {
        super();
        this.name = name;
        this.description = description;
        this.age = age;
        this.sex = sex;
        this.marriage = marriage;
        this.title = title;
        this.date = date;
    }

    public PersonVO() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "uuid")
    @Uuid
    private String uuid;

    @Column(name = "description")
    private String description;

    @Column(name = "age")
    private int age;

    @Column(name = "sex")
    @Enumerated(EnumType.STRING)
    private Sex sex;

    @Column(name = "marriage")
    private boolean marriage;

    @Column(name = "title")
    private String title;

    @Column(name = "date")
    private Date date;

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public String getDescription() {
        return description;
    }

    public int getAge() {
        return age;
    }

    public Sex getSex() {
        return sex;
    }

    public boolean isMarriage() {
        return marriage;
    }

    public String getTitle() {
        return title;
    }

    public Date getDate() {
        return date;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public void setMarriage(boolean marriage) {
        this.marriage = marriage;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDate(Date date) {
        this.date = date;
    }

}
