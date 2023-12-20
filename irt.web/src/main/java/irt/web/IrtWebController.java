package irt.web;

import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import irt.web.bean.jpa.Filter;
import irt.web.bean.jpa.FilterRepository;
import irt.web.bean.jpa.Product;
import irt.web.bean.jpa.ProductFilter;
import irt.web.bean.jpa.ProductRepository;
import irt.web.bean.jpa.WebMenu;
import irt.web.bean.jpa.WebMenuRepository;

@Controller
@RequestMapping("/")
public class IrtWebController {
	private final Logger logger = LogManager.getLogger();

	@Value("${irt.web.product.par_page}")
	private Integer productParPage;

	@Autowired private EntityManager		entityManager;
	@Autowired private WebMenuRepository	menuRepository;
	@Autowired private FilterRepository		filterRepository;
	@Autowired private ProductRepository	productRepository;

	@GetMapping
    String get(Model model) throws UnknownHostException {
		final List<WebMenu> menus = menuRepository.findByOwnerIdIsNullAndActiveOrderByMenuOrderAsc(true);
//		logger.debug(menus.get(0).getName());
		model.addAttribute("menus", menus);
		return "home";
    }

	@GetMapping("about")
    String about(Model model) throws UnknownHostException {
		final List<WebMenu> menus = menuRepository.findByOwnerIdIsNullAndActiveOrderByMenuOrderAsc(true);
//		logger.debug(menus.get(0).getName());
		model.addAttribute("menus", menus);

		return "about";
    }

	@GetMapping("news-events")
    String events(Model model) throws UnknownHostException {
		final List<WebMenu> menus = menuRepository.findByOwnerIdIsNullAndActiveOrderByMenuOrderAsc(true);
//		logger.debug(menus.get(0).getName());
		model.addAttribute("menus", menus);
		return "news-events";
    }

	@GetMapping("products")
    String products(@RequestParam(name = "filter", required = false) List<Long> selected, 
    				@RequestParam(required = false) String search,  
    				Model model) throws UnknownHostException {

		final int page = 0;
		logger.traceEntry("selected filters: {}; search: {};", selected, search);

		model.addAttribute("search", search);
		model.addAttribute("pageSize", productParPage);
		final List<WebMenu> menus = menuRepository.findByOwnerIdIsNullAndActiveOrderByMenuOrderAsc(true);
		model.addAttribute("menus", menus);

		final List<AbstractMap.SimpleEntry <String, Filter>> selectedFilters = new ArrayList<>();
		final List<Filter> filters = filterRepository.findByOwnerIdIsNullAndActiveOrderByFilterOrderAsc(true);

		// Check Selected Filters
		Optional.ofNullable(selected)
		.ifPresent(
				s->{
					s.forEach(
							filterId->{
								filters.forEach(
										mainFilter->{
											Optional.ofNullable(mainFilter.getSubFilters())
											.ifPresent(
													fs->{
														for(int i=page; i<fs.size(); i++) {
															final Filter f = fs.get(i);
															if(f.getId()==filterId){
																f.setSelected(true);
																selectedFilters.add(new AbstractMap.SimpleEntry <>(mainFilter.getName(), f));
																break;
															}
														}
													});
										});
							});
				});
		model.addAttribute("filters", filters);
		model.addAttribute("selectedFilters", selectedFilters);
		modelAddProducts(model, search, selected, page);

		return "products";
    }

	@GetMapping("products/search")
	String searchProduct(@RequestParam(required = false) List<Long> filter, 
						@RequestParam(required = false) String search, 
						@RequestParam(required = false) Integer page, 
						Model model) throws InterruptedException {

		logger.traceEntry("filter: {}; search: {}; page: {};", filter, search, page);

		modelAddProducts(model, search, filter, page);

		return "products :: products_content";
	}

	@GetMapping("product_detail")
	String productDetai(@RequestParam Long productId, Model model) throws InterruptedException {
		logger.traceEntry("{}", productId);

		productRepository.findById(productId).ifPresent(product->model.addAttribute("product", product));

		return "detail";
	}

	private void modelAddProducts(Model model, String search, List<Long> filterIDs, Integer page) {
		logger.traceEntry("search: {}; filterIDs: {}; page: {};", search, filterIDs, page);

		final Pageable pageable = PageRequest.of(Optional.ofNullable(page).orElse(0), productParPage);

		List<Product> products;

		// No IDs
		final boolean noSearch = search==null || search.isEmpty();
		if(filterIDs==null || filterIDs.isEmpty()) {

			if(noSearch)
				products = productRepository.findByActiveTrueOrderByPartNumberAsc(pageable);

			else 
				products = productRepository.findByNameContainsAndActiveTrueOrderByPartNumberAsc(search, pageable);

			model.addAttribute("products", products);
			return;
		}

		final CriteriaBuilder criteriaBuilder		 = entityManager.getCriteriaBuilder();
		final CriteriaQuery<Product> criteriaQuery	 = criteriaBuilder.createQuery(Product.class);
		final Root<Product> productRoot = criteriaQuery.from(Product.class);
		final Join<ProductFilter, ProductFilter> filterJoin = productRoot.join("productFilters");
		final Path<Long> filterIdPath = filterJoin.get("filterId");

		final List<Predicate> predicates = new ArrayList<>();
		filterIDs.forEach(id -> predicates.add(criteriaBuilder.equal(filterIdPath, id)));
		final Predicate[] array = predicates.toArray(new Predicate[predicates.size()]);
		Predicate where = criteriaBuilder.or(array);

		if(!noSearch) {
			final Predicate like = criteriaBuilder.like(productRoot.get("name"), '%' + search + "%");
			 where = criteriaBuilder.and(like, where);
		}

		
		criteriaQuery.where(where);

		final CriteriaQuery<Product> groupBy = criteriaQuery.groupBy(filterJoin.get("productId"));
		groupBy.having(criteriaBuilder.greaterThanOrEqualTo(criteriaBuilder.count(filterIdPath), (long)filterIDs.size()));
		final TypedQuery<Product> typedQuery = entityManager.createQuery(criteriaQuery);
		products = typedQuery.getResultList();
		logger.error("\n\t Size: {}\n {}", products.size(), products);
		
		
		model.addAttribute("products", products);
	}
}
