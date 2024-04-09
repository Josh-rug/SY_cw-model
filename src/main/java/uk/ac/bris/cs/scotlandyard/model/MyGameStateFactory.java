package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.NonNull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	private final class MyGameState implements GameState {
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		private MyGameState(final GameSetup setup,
							final ImmutableSet<Piece> remaining,
							final ImmutableList<LogEntry> log,
							final Player mrX,
							final List<Player> detectives) {
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;


			if (mrX == null) {
				throw new NullPointerException("mrX is null");
			} else if (detectives == null) {
				throw new NullPointerException("Detectives are null");
			} else if (detectives.isEmpty()) {
				throw new IllegalArgumentException("There is no detectives");
			} else if (setup.graph.nodes().isEmpty()) {
				throw new IllegalArgumentException("The graph is empty");
			}


			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Move is empty!");


			HashSet<Integer> detectiveLocation = new HashSet<>();
			HashSet<String> detectiveColour = new HashSet<>();

			for (Player detective : detectives) {
				if (!detectiveLocation.add(detective.location())) {
					throw new IllegalArgumentException("Detective's locations are overlapped");
				} else if (!detectiveColour.add(detective.piece().webColour())) {
					throw new IllegalArgumentException("Detectives are duplicated");
				} else if (detective.has(ScotlandYard.Ticket.SECRET)) {
					throw new IllegalArgumentException("Detectives cannot have SECRET ticket");
				} else if (detective.has(ScotlandYard.Ticket.DOUBLE)) {
					throw new IllegalArgumentException("Detective cannot have DOUBLE ticket");
				}
			}


		}

		@NonNull
		@Override
		public GameSetup getSetup() {
			return setup;
		}

		@NonNull
		@Override
		public ImmutableSet<Piece> getPlayers() {
			ImmutableSet.Builder<Piece> remaining = new ImmutableSet.Builder<>();
			for (Player detective : detectives) {
				remaining.add(detective.piece());
				detective.piece().webColour();
				if (detectives.contains(detective.piece())) {
					throw new IllegalArgumentException("Detectives are duplicated");
				}
			}
			remaining.add(mrX.piece());

			return remaining.build();
		}

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			for (Player player : detectives) {
				if (player.piece() == detective) {
					return Optional.of(player.location());
				}
			}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			if (piece.equals(mrX.piece())) {

				return Optional.of(ticket -> mrX.tickets().getOrDefault(ticket, 0));
			} else {
				for (Player detective : detectives) {
					if (detective.piece().equals(piece))
						return Optional.of(ticket -> detective.tickets().getOrDefault(ticket, 0));
				}

			}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			winner = ImmutableSet.of();
			return winner;
		}

		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
			HashSet<Move.SingleMove> singleMove = new HashSet<>();
			Set<Integer> detectivesLocation = detectives.stream().map(Player::location).collect(Collectors.toSet());

			for (int destination : setup.graph.adjacentNodes(source)) {
				if (detectivesLocation.contains(destination)) {
					return singleMove;
				}
				for (ScotlandYard.Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
					if (player.tickets().getOrDefault(t.requiredTicket(),0) > 0){
						singleMove.add(new Move.SingleMove(player.piece(),source,t.requiredTicket(),destination));
					}
					if(player.has(ScotlandYard.Ticket.SECRET)){
						singleMove.add(new Move.SingleMove(player.piece(),source,ScotlandYard.Ticket.SECRET,destination));
					}
				}
			}
			return singleMove;
		}

		private static Set<Move.DoubleMove> makeDoubleMove(GameSetup setup, List<Player> detectives, Player mrX,  int source) {
			HashSet<Move.DoubleMove> doubleMove = new HashSet<>();
			if (mrX.has(ScotlandYard.Ticket.DOUBLE)) {
				Set<Move.SingleMove> firstMove = makeSingleMoves(setup, detectives, mrX, source);

				for (Move.SingleMove firstMoves : firstMove) {
					int destination1 = firstMoves.destination;
					Set<Move.SingleMove> secondMove = makeSingleMoves(setup, detectives, mrX, destination1);

					for (Move.SingleMove secondMoves : secondMove) {
						if (mrX.tickets().getOrDefault(firstMoves.ticket, 0) > 0 && mrX.tickets().getOrDefault(secondMoves.tickets(), 0) > 0) {
							int destination2 = secondMoves.destination;
							doubleMove.add(new Move.DoubleMove(mrX.piece(), source, firstMoves.ticket, destination1, secondMoves.ticket, destination2));
						}
					}
				}
			} return doubleMove;
		}

//		private static Set<Move.SingleMove> findValidMove(GameSetup setup,List<Player> detectives,Player mrX){
//			HashSet<Move.SingleMove> detectiveValidMove = new HashSet<>();
//		}



		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			ImmutableSet.Builder<Move> moveBuilder = new ImmutableSet.Builder<>();
			if(mrX.isMrX()){
				int currentLocation = mrX.location();
				Set<Move.SingleMove> singleMoves = makeSingleMoves(setup,detectives,mrX,currentLocation);
				moveBuilder.addAll(singleMoves);
				Set<Move.DoubleMove> doubleMoves = makeDoubleMove(setup,detectives,mrX,currentLocation);
				moveBuilder.addAll(doubleMoves);
				return moveBuilder.build();
			} else {
				for(Player detective : detectives){
					int currentLocation = detective.location();
					Set<Move.SingleMove> singleMoves = makeSingleMoves(setup,detectives,detective,currentLocation);
					moveBuilder.addAll(singleMoves);
				}
			}
			return moveBuilder.build();
		}


		@NonNull
		@Override
		public GameState advance(Move move) {
			return new MyGameState(setup, ImmutableSet.of(), ImmutableList.of(), mrX, detectives);
		}
	}
	@Nonnull
	@Override
	public GameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(), ImmutableList.of(), mrX, detectives);

	}
}
