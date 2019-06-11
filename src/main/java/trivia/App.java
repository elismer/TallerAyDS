package trivia;

import org.javalite.activejdbc.Model;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.halt;
import static spark.Spark.before;
import static spark.Spark.after;
import static spark.Spark.options;

import java.util.ArrayList;
import java.util.HashSet;

import org.javalite.activejdbc.LazyList;

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.DB;

import trivia.*;

import com.google.gson.Gson;
import java.util.Map;
import java.util.List;

class QuestionParam {
	String description;
	ArrayList<Option> options;
	String category_id;
	Boolean active;
	Boolean answered;
}

class UserParam {
	String nickName;
	String pasword;
}

public class App {

	static LazyList<Option> options;

	static User currentUser;

	public static void main(String[] args) {

		before((request, response) -> {
			if (Base.hasConnection()){
				Base.close();
			}
			if (!Base.hasConnection())
			//Base.open("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/trivia_dev", "root", "root");
			Base.open();
			String headerToken = (String) request.headers("Authorization");
			System.out.println("headerToken: " + headerToken);
			if (headerToken == null || headerToken.isEmpty() || !BasicAuth.authorize(headerToken)) {
				halt(401);
			}

			currentUser = BasicAuth.getUser(headerToken);
		});

		after((request, response) -> {
			Base.close();
			response.header("Access-Control-Allow-Origin", "*");
			response.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
			response.header("Access-Control-Allow-Headers",
					"Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,");
		});

		options("/*", (request, response) -> {
			return "OK";
		});

		post("/login", (req, res) -> {
			res.type("application/json");

			// if there is currentUser is because headers are correct, so we only
			// return the current user here
			return currentUser.toJson(true);
		});

		/*
		 * get("/hello/:name", (req, res) -> { return "hello" + req.params(":name"); });
		 *
		 * System.out.println("hola mundo");
		 */

		post("/users", (req, res) -> {
			Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);

			User user = new User();
			user.set("nick_name", bodyParams.get("nick_name"));
			user.set("dni", bodyParams.get("dni"));
			user.set("username", bodyParams.get("username"));
			user.set("last_name", bodyParams.get("last_name"));
			user.set("password", bodyParams.get("password"));
			user.set("year", bodyParams.get("year"));
			user.saveIt();

			Game game = new Game();
			Stat stat = new Stat();
			user.add(game);
			user.add(stat);
			res.type("application/json");

			return user.toJson(true);

		});

		get("/users", (req, res) -> { // retorna todos los usuarios
			LazyList<User> user = User.findAll();
			for (User u : user)
				System.out.println("Su username es: " + u.get("username") + ", su dni es: " + u.get("dni"));
			return user;
		});

		get("/game/:category_id", (req, res) -> {
			Category category = Category.findById(req.params("category_id"));
			LazyList<Game> games = currentUser.getAll(Game.class);// Sacamos el juego del usuario
			Game game = games.get(0);
			LazyList<Option> optionsCorrects = game.get(Option.class, "type = ?", "CORRECT");// Obtenemos las respuestas
																								// que fueron correctas
			HashSet<Integer> idsOfQuestions = new HashSet<Integer>();// Creamos un conjunto para tener los id's de las
																		// preguntas
			for (Option o : optionsCorrects) {
				idsOfQuestions.add((int) o.get("question_id"));// Agragamos los id's al conjunto
			}
			LazyList<Question> unansweredQuestions = category.getAll(Question.class);// Creamos un copia de todas las
			// preguntas
			for (int i = 0; i < unansweredQuestions.size(); i++) {// Para todas las preguntas de la categoria
				Question q = unansweredQuestions.get(i);
				if (idsOfQuestions.contains(q.get("id"))) {// Si su id esta en el conjunto de las respondidas
					unansweredQuestions.remove(i);// Lo sacamos del conjunto de las no respondidas
					i--;
				}
			}
			Question question = new Question();
			if (!unansweredQuestions.isEmpty()) {
				int cant = (int) (Math.random() * unansweredQuestions.size());
				question = unansweredQuestions.get(cant);
			}
			// Question question = AllQuestionsOfCategory.get(cant);
			/*
			 * System.out.println("Descripcion de la pregunta: " +
			 * question.get("description")); options = question.getAll(Option.class); for
			 * (Option o : options) System.out.println(o.get("description"));
			 */
			// return question.toJson(true);
			return question.toJson(true);
		});

		post("/questions", (req, res) -> {
			QuestionParam bodyParams = new Gson().fromJson(req.body(), QuestionParam.class);
			Question question = new Question();
			Category category = Category.findById(bodyParams.category_id);
			question.set("description", bodyParams.description);
			category.add(question);

			question.saveIt();

			for (Option item : bodyParams.options) {
				Option option = new Option();
				option.set("description", item.description);
				option.set("type", item.type);
				question.add(option);
			}

			return question.toJson(true);
		});

		get("/options/:question_id", (req, res) -> {
			Question question = Question.findById(req.params("question_id"));
			LazyList<Option> options = question.getAll(Option.class);
			return options.toJson(true);
		});

		get("/questions/options", (req, res) -> { // retorna todas las preguntas con sus opciones
			LazyList<Question> question = Question.findAll();
			for (Question q : question) {
				System.out.println("Descripcion de la pregunta: " + q.get("description"));
				LazyList<Option> option = q.getAll(Option.class);
				for (Option o : option)
					System.out.println("Descripcion de la opcion: " + o.get("description") + " tipo de la opcion: "
							+ o.get("type"));
			}
			return question.toJson(true);
		});

		post("/comments", (req, res) -> {
			Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);

			Comment comment = new Comment();
			comment.set("description", bodyParams.get("description"));
			currentUser.add(comment);

			res.type("application/json");

			return comment.toJson(true);
		});

		get("/comments", (req, res) -> {
			LazyList<Comment> com = Comment.findAll();
			for (Comment comment : com) {
				System.out.println(comment);
			}

			return com;
		});

		get("/categories", (req, res) -> {
			LazyList<Category> cat = Category.findAll();
			for (Category category : cat) {
				System.out.println(category);
			}
			return cat;
		});

		get("/stats", (req, res) -> {

			LazyList<Stat> stats = currentUser.getAll(Stat.class);
			Stat stat = stats.get(0);

			res.type("application/json");

			return stat.toJson(true);
		});

		post("/answers", (req, res) -> {
			Map<String, Object> bodyParams = new Gson().fromJson(req.body(), Map.class);
			Answer answer = new Answer();
			LazyList<Game> games = currentUser.getAll(Game.class);
			Game game = games.get(0);
			Option option = Option.findById(bodyParams.get("chosen_option"));
			game.add(option);
			if (option.get("type").equals("CORRECT")) {
				System.out.println("Tu respuesta es correcta");
			} else if (option.get("type").equals("INCORRECT"))
				System.out.println("Respuesta incorrecta");

			res.type("application/json");

			return answer.toJson(true);
		});

	}
}
