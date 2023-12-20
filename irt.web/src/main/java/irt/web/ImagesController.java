package irt.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("images")
public class ImagesController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.web.product.images.path}")
	private String productImagesPath;

	@Value("${irt.web.product.images.default}")
	private String defaultImage;

	@Value("${irt.web.source.path}")
	private String sourcePath;

	@GetMapping("product/{productId}/{imageStatus}/{subfolder}/{fileName}")
	public ResponseEntity<Resource> getImage(@PathVariable  Long productId, @PathVariable ImageStatus imageStatus, @PathVariable Long subfolder, @PathVariable String fileName) throws IOException{
		logger.traceEntry("productId: {}; imageStatus: {}; subfolder: {}; fileName: {}", productId, imageStatus, subfolder, fileName);

		final Path path = Paths.get(productImagesPath, productId.toString(), imageStatus.toString(), subfolder.toString(), fileName);

		return getImage(path);
	}

	@GetMapping("product/{productId}/{imageStatus}/{fileName}")
	public ResponseEntity<Resource> getImage(@PathVariable  Long productId, @PathVariable ImageStatus imageStatus, @PathVariable String fileName) throws IOException{
		logger.traceEntry("productId: {}; imageStatus: {}; fileName: {}", productId, imageStatus, fileName);

		final Path path = Paths.get(productImagesPath, productId.toString(), imageStatus.toString(), fileName);

		return getImage(path);
	}

	@GetMapping("/uri/{productId}")
	public List<URI> getImagePath(@PathVariable  Long productId) throws IOException, URISyntaxException{

		return getActiveImageUri(sourcePath, productImagesPath, productId, defaultImage);
	}

	private ResponseEntity<Resource> getImage(final Path path) throws FileNotFoundException {
		final File file = path.toFile();

		HttpHeaders headers = getHeader();
		headers.add("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");

		if(!file.exists()) {
			return ResponseEntity.notFound()
					.headers(headers)
					.build();
		}

		final InputStream is = new FileInputStream(file);

		return ResponseEntity.ok()
				.headers(headers)
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(new InputStreamResource(is));
	}

	@PostMapping(path="/product/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public String addImages(@RequestParam Long productId, @RequestPart("fileToAttach") List<MultipartFile> files) {
		logger.traceEntry("productIdL {}", productId);

		String subfolderName = Long.toString(System.currentTimeMillis());
		files.stream().forEach(saveImage(productId, subfolderName));

		return"Done";
	}

	private Consumer<? super MultipartFile> saveImage(Long productId, String subfolderName) {
		return  mpFile->{

			try {
				String originalFilename = mpFile.getOriginalFilename();
				Path path = Paths.get(productImagesPath, productId.toString(), ImageStatus.ACTIVE.toString(), subfolderName);

				path.toFile().mkdirs();	//create a directories

				path = Paths.get(path.toString(), originalFilename);

				try { mpFile.transferTo(path); } catch (IllegalStateException | IOException e) { logger.catching(e); }
			} catch (Exception e) {
				logger.catching(e);
			}

		};
	}

	private HttpHeaders getHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		headers.add("Expires", "0");
		return headers;
	}

	public static List<URI> getActiveImageUri(String sourcePath, String productImagesPath, Long productId, String defaultImage) throws URISyntaxException, IOException {
		logger.traceEntry("sourcePath:{}; productImagesPath: {}; productId:{}; defaultImage: {}", sourcePath, productImagesPath, defaultImage);

		final Path path = Paths.get(productImagesPath, productId.toString(), ImageStatus.ACTIVE.toString());

		if(!path.toFile().exists()) {

			final List<URI> list = new ArrayList<>();
			list.add(new URI(defaultImage));
			return list;
		}

		try (Stream<Path> stream = Files.walk(path)) {

			final List<URI> collect = stream.filter(Files::isRegularFile).map(Path::toUri).map(uri->Paths.get(sourcePath).toUri().relativize(uri)).collect(Collectors.toList());
			if(collect.isEmpty())
				collect.add(new URI(defaultImage));

			return collect;
		}
	}

	@RequiredArgsConstructor
	public enum ImageStatus{
		ACTIVE("active"),
		DISABLED("disabled"),
		UNKNOWN("unknown");

		private final String status;

		public String toString() {
			return status;
		}

		public static ImageStatus convert(String source) {

			for(ImageStatus is: values()) {
				if(is.status.equals(source))
					return is;
			}

			return UNKNOWN;
		}
	}
}
