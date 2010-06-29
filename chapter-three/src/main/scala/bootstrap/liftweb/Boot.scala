package bootstrap.liftweb

// framework imports
import net.liftweb.common._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import net.liftweb.mapper.{DB,Schemifier,DefaultConnectionIdentifier,StandardDBVendor,MapperRules}

// app imports
import example.travel.model.{Auction,Supplier,Customer,Bid,Order,OrderAuction}
// import example.travel.lib.{Helpers}

class Boot extends Loggable {
  def boot {
    LiftRules.addToPackages("example.travel")
    
    MapperRules.columnName = (_,name) => StringHelpers.snakify(name)
    MapperRules.tableName =  (_,name) => StringHelpers.snakify(name)
    
    // set the JNDI name that we'll be using
    DefaultConnectionIdentifier.jndiName = "jdbc/liftinaction"
    
    // handle JNDI not being avalible
    if (!DB.jndiJdbcConnAvailable_?){
      logger.error("No JNDI configured - using the default in-memory database.") 
      DB.defineConnectionManager(DefaultConnectionIdentifier, Application.database)
      // make sure cyote unloads database connections before shutting down
      LiftRules.unloadHooks.append(() => Application.database.closeAllConnections_!()) 
    }
    
    // automatically create the tables
    Schemifier.schemify(true, Schemifier.infoF _, 
      Bid, Auction, Supplier, Customer, Order, OrderAuction)
    
    // setup the 404 handler 
    LiftRules.uriNotFound.prepend(NamedPF("404handler"){
      case (req,failure) => NotFoundAsTemplate(ParsePath(List("404"),"html",false,false))
    })
    
    // LiftRules.loggedInTest = Full(() => User.loggedIn_?)
    
    // set the application sitemap
    // LiftRules.setSiteMap(SiteMap(Application.sitemap:_*))
    
    // def sitemap = SiteMap(
    //   Menu("Home") / "index" >> LocGroup("public"),
    //   Menu("Search") / "search" >> LocGroup("public"),
    //   Menu("History") / "history" >> LocGroup("public"),
    //   Menu("Auctions") / "auctions" >> LocGroup("public"),
    //   Menu("Auction Detail") / "auction" >> LocGroup("public"),
    //   // admin
    //   Menu("Admin") / "admin" / "index" >> LocGroup("admin"),
    //   Menu("Suppliers") / "admin" / "suppliers" >> LocGroup("admin") submenus(Supplier.menus : _*),
    //   Menu("Auction Admin") / "admin" / "auctions" >> LocGroup("admin") submenus(Auction.menus : _*),
    // )
    // LiftRules.setSiteMap(sitemap)
    
    LiftRules.setSiteMap(SiteMap(Application.sitemap: _*))
    
    
    // setup the load pattern
    S.addAround(DB.buildLoanWrapper)
    
    // make requests utf-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))
  }
}

object Application {
  val sitemap = List(
    Menu("Home") / "index" >> LocGroup("public"),
    Menu("Search") / "search" >> LocGroup("public"),
    Menu("History") / "history" >> LocGroup("public"),
    Menu("Auctions") / "auctions" >> LocGroup("public"),
    Menu("Auction Detail") / "auction" >> LocGroup("public") >> Hidden,
    // admin
    Menu("Admin") / "admin" / "index" >> LocGroup("admin"),
    Menu("Suppliers") / "admin" / "suppliers" >> LocGroup("admin") submenus(Supplier.menus : _*),
    Menu("Auction Admin") / "admin" / "auctions" >> LocGroup("admin") submenus(Auction.menus : _*)
  ) ::: Customer.menus
  
  val database = DBVendor
  
  object DBVendor extends StandardDBVendor(
    Props.get("db.class").openOr("org.h2.Driver"),
    Props.get("db.url").openOr("jdbc:h2:database/chapter_three;DB_CLOSE_DELAY=-1"),
    Props.get("db.user"),
    Props.get("db.pass"))
  
}



