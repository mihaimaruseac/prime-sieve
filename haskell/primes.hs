import System.Environment

data MultiSieve = MS Integer [Integer] deriving Show

primes :: [Integer]
primes = small ++ large
  where
    p:candidates = sieve $ multiSieve small
    small          = [2, 3, 5, 7, 11, 13]
    large          = p : filter isPrime candidates
    isPrime n      = all (\p-> n `mod` p /= 0) $ takeWhile (\p -> p*p <= n) large

multiSieve bootstrap  = foldl next (MS 1 [2]) bootstrap

next (MS sz primes) p = MS (sz * p) [p'' | k <- [0.. p - 1], p' <- primes, let p'' = sz * k + p', p'' `mod` p /= 0]

sieve (MS sz primes) = [sz * k + p | k <- [0..], p <- primes]

main = do
  args <- getArgs
  case args of
    [] -> mapM (\(x,y) -> putStrLn $ show x ++ " " ++ show y) $ filter (\(x,_) -> x `mod` 10000 == 0) $ zip [1..] primes
    x:_ -> mapM print $ take (read x) primes
