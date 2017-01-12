class Boxpile < Formula
  desc "A pile of containers linked together"
  homepage "https://github.com/nikogura/boxpile"
  url "https://"
  sha256 ""
  version "0.1.0"

  def install
    share.install "boxpile-0.1.0-jar-with-dependencies.jar"
    bin.install "boxpile"

    ohai "Boxpile Installed. Muahahahahahahahahahaha!"
    ohai ""

  end
end