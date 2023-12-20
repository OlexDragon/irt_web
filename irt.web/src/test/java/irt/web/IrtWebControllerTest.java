package irt.web;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import irt.web.bean.jpa.Product;
import irt.web.bean.jpa.ProductFilter;


@SpringBootTest
public class IrtWebControllerTest {
	private final Logger logger = LogManager.getLogger();

	@Autowired private EntityManager		entityManager;

	@Transactional
	@Test
	public void test() {
		String search = "80W";

		final CriteriaBuilder criteriaBuilder		 = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Product> criteriaQuery	 = criteriaBuilder.createQuery(Product.class);
		final Root<Product> productRoot = criteriaQuery.from(Product.class);
		final Join<ProductFilter, ProductFilter> filterJoin = productRoot.join("productFilters");
		final Path<Long> filterIdPath = filterJoin.get("filterId");
		final Predicate equal1 = criteriaBuilder.equal(filterIdPath, 12L);
		final Predicate equal2 = criteriaBuilder.equal(filterIdPath, 9L);
		final Predicate like = criteriaBuilder.like(productRoot.get("name"), '%' + search + "%");
		final Predicate orPredicate = criteriaBuilder.or(equal1, equal2);
		final Predicate oandPredicate = criteriaBuilder.and(like, orPredicate);
		
		criteriaQuery.where(oandPredicate);
		final CriteriaQuery<Product> groupBy = criteriaQuery.groupBy(filterJoin.get("productId"));
		groupBy.having(criteriaBuilder.greaterThanOrEqualTo(criteriaBuilder.count(filterIdPath), 2L));
		final TypedQuery<Product> typedQuery = entityManager.createQuery(criteriaQuery);
		List<Product> products = typedQuery.getResultList();
		logger.error("\n\t Size: {}\n {}", products.size(), products);
	}

}
