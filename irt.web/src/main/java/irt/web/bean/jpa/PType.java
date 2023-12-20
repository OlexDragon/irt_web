package irt.web.bean.jpa;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "p_type")
@Getter @Setter @ToString
public class PType {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long	 id;
	private String type;
	private String description;
}
