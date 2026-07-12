export default function Footer() {
  return (
    <footer className="footer">
      <div className="container">
        <span>© {new Date().getFullYear()} SkyBook — a portfolio flight booking demo.</span>
        <a href="https://www.postman.com" target="_blank" rel="noopener noreferrer">
          Explore the API on Postman →
        </a>
      </div>
    </footer>
  );
}
